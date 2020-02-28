package net.iesseveroochoa.manuelmartinez.practica08;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static final int MY_PERMISSIONS_REQUEST_READ_LOCATION = 1;
    private static final int REQUEST_CONFIG_UBICACION = 201;
    private static final String LOGTAG = "";
    TextView tvLongitud;
    TextView tvLatitud;
    //para leer la ultima localizacion conocida
    private FusedLocationProviderClient ultimaLocalizacionConocida;
    ToggleButton tbActualizar;
    //Nos permite determinar la calidad de la geolocalización
    LocationRequest mLocationRequest;
    //Evento que permitirá obtener actualizaciones
    private LocationCallback mLocationCallback;
    //Variables para obtener y guardar las posiciones en un mapa
    double latitud;
    double longitud;
    LatLng inicio;
    LatLng fin;
    //Variable para crear el mapa
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvLatitud = findViewById(R.id.tvLatitud);
        tvLongitud = findViewById(R.id.tvLongitud);
        tbActualizar = findViewById(R.id.tbIniciar);

        //Inicializamos las posiciones por defecto
        latitud = 38.0846;
        longitud = -0.9431;

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map2);
        mapFragment.getMapAsync(this);

        //instanciamos el servicio de geolocalización
        ultimaLocalizacionConocida = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_READ_LOCATION);
        }


        // Activamos y desactivamos la localización con el botón
        tbActualizar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton,
                                         boolean enable) {
                if (enable) {
                    activaLocationUpdates();
                    obtenerPosicion();
                    solicitarUltimaLocalizacion();
                } else {
                    desactivaLocationUpdates();
                    obtenerPosicion();
                    insertarMarcadorFinal();
                    dibujarLineas();
                    //animarPuntoFinalRuta();

                }
            }
        });

    }

    /**
     * inicializa el mapa y los puntos sobre este
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

    }

    /**
     * Metodo para obtener la posicion
     */
    private void obtenerPosicion() {
        CameraPosition camPos = mMap.getCameraPosition();
        LatLng coordenadas = camPos.target;
        latitud = coordenadas.latitude;
        longitud = coordenadas.longitude;
        fin = new LatLng(latitud, longitud);
        Toast.makeText(this, "Lat: " + latitud + " | Long: " + longitud, Toast.LENGTH_SHORT).show();

    }

    /**
     * Insertar marcadores
     */
    private void insertarMarcadorInicial() {
        mMap.addMarker(new MarkerOptions().position(inicio).title("Inicio"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(inicio));
        //animarPuntoInicialRuta();

    }

    private void insertarMarcadorFinal() {
        mMap.addMarker(new MarkerOptions().position(fin).title("Final"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(fin));
        //dibujarLineas();
        //animarPuntoFinalRuta();
    }

    /**
     * Metodo para animar el mapa
     */
    private void animarPuntoFinalRuta() {
        //fin = new LatLng(40.417325, -3.683081);

        CameraPosition camPos = new CameraPosition.Builder()
                .target(fin)   //Centramos el mapa en el ultimo punto
                .zoom(19)         //Establecemos el zoom en 19
                .bearing(45)      //Establecemos la orientación con el noreste arriba
                .tilt(45)         //Bajamos el punto de vista de la cámara 45 grados
                .build();

        CameraUpdate camUpd3 =
                CameraUpdateFactory.newCameraPosition(camPos);

        mMap.animateCamera(camUpd3);
    }

    private void animarPuntoInicialRuta() {
        CameraPosition camPos = new CameraPosition.Builder()
                .target(inicio)   //Centramos el mapa en el primer punto
                .zoom(19)         //Establecemos el zoom en 19
                .bearing(45)      //Establecemos la orientación con el noreste arriba
                .tilt(45)         //Bajamos el punto de vista de la cámara 45 grados
                .build();

        CameraUpdate camUpd3 =
                CameraUpdateFactory.newCameraPosition(camPos);

        mMap.animateCamera(camUpd3);
    }

    /**
     * Dibujar lineas en el mapa que unen los puntos de una ruta
     */
    private void dibujarLineas() {
        //Dibujo con Lineas
        PolylineOptions lineas = new PolylineOptions()
                .add(inicio)
                .add(fin);
        lineas.width(8);
        lineas.color(Color.RED);
        mMap.addPolyline(lineas);
        animarPuntoFinalRuta();
    }

    /**
     * mostramos la localización en pantalla
     */
    private void actualizaLocalizacionUI(Location loc) {
        if (loc != null) {
            tvLatitud.setText("Latitud: " +
                    String.valueOf(loc.getLatitude()));
            latitud = loc.getLatitude();
            tvLongitud.setText("Longitud: " +
                    String.valueOf(loc.getLongitude()));
            longitud = loc.getLongitude();
        } else {
            tvLatitud.setText("Latitud: (desconocida)");
            tvLongitud.setText("Longitud: (desconocida)");
        }
    }

    /**
     * Nos permite crear una solicitud de última locacización conocida
     */
    private void solicitarUltimaLocalizacion() {
        ultimaLocalizacionConocida.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                actualizaLocalizacionUI(location);
                inicio = new LatLng(location.getLatitude(), location.getLongitude());
                insertarMarcadorInicial();

            }
        });
    }

    /**
     * Controla la respuesta al solicitar los permisos
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    /**
     * Define el evento para la recepción de nuevas localizaciones
     */
    private void crearEventoRecepcionLocalizaciones() {
        //definimos el evento de recepcion de localizaciones
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Actualizamos la nueva localizacion
                    actualizaLocalizacionUI(location);
                }
            }
        };
    }

    /**
     * activamos las actualizaciones GPS
     */
    private void activaLocationUpdates() {
        if (mLocationCallback == null)
            crearEventoRecepcionLocalizaciones();
        //definimos los parametros de actualización
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(2000);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY
        );
        //Creamos el builder de solicitud que permitirá al sistema comprobar los requerimientos solicitados
        LocationSettingsRequest.Builder builder = new
                LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        SettingsClient client =
                LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task =
                client.checkLocationSettings(builder.build());
        //añadimos la tarea en la que definimos que hacemos cuando si se cumplen los requerimientos
        task.addOnSuccessListener(this, new
                OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse
                                                  locationSettingsResponse) {
                        // All location settings are satisfied. The client can initialize
                        // location requests here.
                        //solicitamos las actualizacion de localizaciones

                        ultimaLocalizacionConocida.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, null /* Looper */);
                    }
                });
        //indicamos que hacemos si falla. En nuestro caso le pedimos al usuario que active GPS
        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this, REQUEST_CONFIG_UBICACION);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    //si no cumplimos con las necesidades de Localización solicitamos al usuario que la pida de tal forma que el resultado
    //lo vemos aquí
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONFIG_UBICACION:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        activaLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(LOGTAG, "El usuario no ha realizado los cambios de configuración necesarios");
                        //si no cumplimos deberíamos enviar un mensaje al usuario explicando por qué lo necesitas
                        tbActualizar.setChecked(false);
                        break;
                }
                break;
        }
    }

    /**
     * desactivamos las actualizaciones GPS
     */
    private void desactivaLocationUpdates() {
        ultimaLocalizacionConocida.removeLocationUpdates(mLocationCallback);
    }


}
