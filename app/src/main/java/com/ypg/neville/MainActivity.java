package com.ypg.neville;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentContainerView;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.ypg.neville.Ui.frag.frag_content_WebView;
import com.ypg.neville.Ui.frag.frag_home;
import com.ypg.neville.Ui.frag.frag_listado;
import com.ypg.neville.model.db.DatabaseHelper;
import com.ypg.neville.model.db.utilsDB;
import com.ypg.neville.model.utils.QRManager;
import com.ypg.neville.model.utils.UiModalWindows;
import com.ypg.neville.model.utils.Utils;
import com.ypg.neville.model.utils.myListener_In_App_Update;
import com.ypg.neville.model.utils.utilsFields;
import com.ypg.neville.services.serviceStreaming;

import java.util.List;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    serviceStreaming mservise;

    public static MainActivity mainActivityThis;


    DrawerLayout drawerLayout;
    NavigationView navigationView;
    public ChipNavigationBar bottomNavigationView;
    Toolbar toolbar;
    ActionBarDrawerToggle toggle;

    TextView fraseBienvenida;
    ImageView headerImage;

    private FirebaseAnalytics firebaseAnalytics;


//este es el nav controles Yorjandis
    public NavController navController;


    //iconos de la  toolsbar:
    public ImageView ic_toolsBar_nota_add, ic_toolsBar_frase_add, ic_toolsBar_fav;

    public static String version = ""; //Almacena la version de la app publicada
    Utils utils = new Utils(MainActivity.this); //clase para las funciones útiles


    public static String prefijo = "";  //variable que almacena el tipo de fichero a cargar (conf_=conferencia, biog_=biografia, etc)
    FragmentContainerView frag_container;


    //PERMISSION request constant, assign any value
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final String TAG = "PERMISSION_TAG";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MainActivity.mainActivityThis = MainActivity.this; //Almacenando una referencia a esta clase


        //Setting: Estableciendo el tema en la app
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("tema", true)) {
            setTheme(R.style.Theme_NevilleProyect_noche);
        } else {
            setTheme(R.style.Theme_NevilleProyect);
        }

        setContentView(R.layout.activity_main);


        //Configurando los iconos de la barra de tareas
        ic_toolsBar_nota_add = findViewById(R.id.ic_toolbar_add_note);
        ic_toolsBar_frase_add = findViewById(R.id.ic_toolbar_add_frase);
        ic_toolsBar_fav = findViewById(R.id.ic_toolbar_fav);


        //Setup UI component
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolsbar);
        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        frag_container = findViewById(R.id.frag_container);

        View navigationHeader = navigationView.getHeaderView(0); //para tener acceso a los elementos del header
        fraseBienvenida = navigationHeader.findViewById(R.id.drawer_header_frase);
        headerImage = navigationHeader.findViewById(R.id.drawer_header_imgbutton);



        //Setting: Estableciendo el color de los marcos de la app
        int temp_Color = PreferenceManager.getDefaultSharedPreferences(this).getInt("color_marcos", 0);
        AuxSetColorBar(temp_Color);


        //Aplicando el mensaje de vienvenida al Header del navigation View
        fraseBienvenida.setText(PreferenceManager.getDefaultSharedPreferences(this).getString("frase", "Imaginar crea la realidad"));
        headerImage.setClipToOutline(true);


        //Para navegar por los fragments
        navController = Navigation.findNavController(frag_container);


        //Para In_App_Update from google store
        myListener_In_App_Update in_app_update = new myListener_In_App_Update(MainActivity.this);

        in_app_update.setMylistener(new myListener_In_App_Update.In_mylistener() {
            @Override
            public void onUpdateAvailable(Boolean pUpdateAvailable) {
                if (pUpdateAvailable) {
                    Intent intentNotification = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getApplicationContext().getPackageName()));
                    utils.show_Notification("Nueva actualización disponible!", intentNotification);
                }
            }
        });


        //Determinando si las tablas (frases, conferencias) de la BD estan llenas. Si están vacias, las popula automáticamente
        if (utilsDB.RestoreDBInfo(MainActivity.this)) {
            //Si llegó a popular alguna tabla, reinicia la activity
            Intent intent = getIntent();
            //intent.putExtra("STRINGTOSAVE", "Contenido restaurado"); //Línea Opcional para informar de la operación
            finish();
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        } else {

            //Setting : Mostrando las News por primera vez, solo la primera vez, jjj
            if (PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("Is_primeraVez", true)) {
                UiModalWindows.showAyudaContectual(MainActivity.this, "Novedades", "Que hay de nuevo?", getString(R.string.news), false, getDrawable(R.drawable.neville));
                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean("Is_primeraVez", false).apply();
            }

        }


        //(SOLO UNA VEZ) Corrigiendo errores ortográficos en la tabla de frases) además inserta las nuevas frases añadidas a la BD
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("updateFrases", true)) {

            utilsDB.CorrectOrtogFrases(MainActivity.this);

            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("updateFrases", false).apply();
        }

        //:::::::::::::::::::::::::::::::::::::::   ZONA DE TEST ::::::::::::::::::::::::::::::::::::::::::::



        //:::::::::::::::::::::::::::::::::::::::   FIN ZONA DE TEST ::::::::::::::::::::::::::::::::::::::::::::


        //evento click del botón add frase de la toolbar
        ic_toolsBar_frase_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UiModalWindows.Add_New_frase(MainActivity.this, null);
            }
        });


        ic_toolsBar_nota_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              UiModalWindows.ApunteManager(MainActivity.this,"", null, false);
            }
        });

        //evento onclick del icono de favoritos toolbar
        ic_toolsBar_fav.setOnClickListener(view -> {

            String result = "";

            String fragName = frag_container.getFragment().getClass().getSimpleName();

            if( fragName.contains("frag_content_WebView")){
                result = utilsDB.UpdateFavorito(this, DatabaseHelper.T_Conf, DatabaseHelper.C_conf_title, utilsFields.ID_Str_row_ofElementLoad, -1);

            }else if(fragName.contains("frag_listado")){ //si se esta mostrando el frag_listado

                switch (frag_listado.elementLoaded){
                    case "video_conf":
                    case "video_book":
                    case "video_gredd":
                        result = utilsDB.UpdateFavorito(this, DatabaseHelper.T_Videos, DatabaseHelper.C_videos_title, utilsFields.ID_Str_row_ofElementLoad, -1);
                        break;
                    case "video_ext":
                    case "audio_ext":
                        result = utilsDB.UpdateFavorito(this, DatabaseHelper.T_Repo, DatabaseHelper.C_repo_title, utilsFields.ID_Str_row_ofElementLoad, -1);
                        break;
                }
            }

            if (result != "") {
                setFavColor(result);
            }

        });


        //Setup Toolsbar
        setSupportActionBar(toolbar);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.nav_cerrado, R.string.nav_abierto);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        //Eventos de los items en el navigation drawer
        navigationView.setNavigationItemSelectedListener(item -> {

            Bundle bundleInfo = new Bundle(); //Almacena la información pertinente

            Intent intent;

            deselecItemBottom(); // deseleccionar los iconos en la botón bar

            //eventos de los items del drawer
            switch (item.getItemId()) {
                case (R.id.drawer_menu_biografia):
                    frag_content_WebView.elementLoaded = "biografia";
                    frag_content_WebView.extension = ".html";
                    frag_content_WebView.urlPath = "file:///android_asset/biog_quien es neville goddard.html";
                    navController.navigate(R.id.frag_content_webview);

                   // QRManager.ShowQRDialog(MainActivity.this, "Esto es solo un ejemplo de lo que podemos hacer", "Compartir Frase", null);

                    break;
                case (R.id.drawer_menu_galeriafotos):
                    frag_content_WebView.elementLoaded = "galeriafotos";
                    frag_content_WebView.extension = ".html";
                    frag_content_WebView.urlPath = "file:///android_asset/gale_Galeria de fotos.html";
                    navController.navigate(R.id.frag_content_webview);
                    break;
                case (R.id.drawer_menu_abdullah): //Online
                    if (!Utils.isConnection(getApplicationContext())){break;}
                    frag_listado.elementLoaded = "play_youtube";
                    frag_listado.urlPath = "mgbdcv606Rg";
                    navController.navigate(R.id.frag_listado);
                    break;
                case (R.id.drawer_menu_conferen_texto):
                    frag_listado.elementLoaded = "conf";
                    bottomNavigationView.setItemSelected(R.id.bottom_menu_conf, true);
                    navController.navigate(R.id.frag_listado);
                    break;
                case (R.id.drawer_menu_offline_videos):
                    obtenerPermisos(); //Obtener los permisos necesarios (Solo una vez yor)
                    frag_listado.elementLoaded = "video_ext";
                    navController.navigate(R.id.frag_listado);
                    break;
                case (R.id.drawer_menu_offline_audios):
                    obtenerPermisos(); //Obtener los permisos necesarios (Solo una vez yor)
                    frag_listado.elementLoaded = "audio_ext";
                    navController.navigate(R.id.frag_listado);
                    break;
                case (R.id.drawer_menu_preguntas):
                    frag_listado.elementLoaded = "preguntas";
                    navController.navigate(R.id.frag_listado);
                    break;
                case (R.id.drawer_menu_citas_conf):
                    frag_listado.elementLoaded = "citasConferencias";
                    navController.navigate(R.id.frag_listado);
                    break;
                case (R.id.drawer_menu_conferen_video)://Online
                    if (!Utils.isConnection(getApplicationContext())){break;}
                    frag_listado.elementLoaded = "video_conf";
                    bottomNavigationView.setItemSelected(R.id.bottom_menu_videos, true);
                    navController.navigate(R.id.frag_listado);
                    break;
                case (R.id.drawer_menu_conferen_audio): //Online
                    if (!Utils.isConnection(getApplicationContext())){break;}
                    intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://www.ivoox.com/escuchar-neville-goddard_nq_102778_1.html"));
                    startActivity(intent);
                    break;
                case (R.id.drawer_menu_frases):
                    frag_home.elementLoaded_home = "frases";
                    bottomNavigationView.setItemSelected(R.id.bottom_menu_citas, true);
                    navController.navigate(R.id.frag_home);
                    break;
                case (R.id.drawer_menu_books)://Online
                    if (!Utils.isConnection(getApplicationContext())){break;}
                    intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://drive.google.com/file/d/1NjUDZfjSOjdPRd6vsyhfDKmjdDus25YM/view?usp=sharing"));
                    startActivity(intent);
                    break;
                case (R.id.drawer_menu_audiobook): //Online
                    if (!Utils.isConnection(getApplicationContext())){break;}
                    frag_listado.elementLoaded = "video_book";
                    bottomNavigationView.setItemSelected(R.id.bottom_menu_books, true);
                    navController.navigate(R.id.frag_listado);
                    break;
                case (R.id.drawer_menu_ayudas):
                    frag_listado.elementLoaded = "ayudas";
                    navController.navigate(R.id.frag_listado);
                    break;

                case (R.id.drawer_menu_gregg):
                    navController.navigate(R.id.frag_gregg);
                    break;
                case (R.id.drawer_menu_audio_telegram): //Online
                    if (!Utils.isConnection(getApplicationContext())){break;}
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/nevilleGoddardaudios"));
                    intent.setPackage("org.telegram.messenger");
                    startActivity(intent);
                    break;
                case (R.id.drawer_menu_web_neville_blog): //Online
                    if (!Utils.isConnection(getApplicationContext())){break;}
                    intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://nevilleenespanol.blogspot.com/"));
                    startActivity(intent);
                    break;
                case (R.id.drawer_menu_web_neville_espanol)://Online
                    if (!Utils.isConnection(getApplicationContext())){break;}
                    intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://neville-espanol.com/"));
                    startActivity(intent);
                    break;
                case (R.id.drawer_menu_web_real_neville): //Online
                    if (!Utils.isConnection(getApplicationContext())){break;}
                    intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://realneville.com/"));
                    startActivity(intent);
                    break;
            }

            drawerLayout.close();


            return false;
        });


        //Eventos de los items en el Bottom navigation
        bottomNavigationView.setOnItemSelectedListener(new ChipNavigationBar.OnItemSelectedListener() {
            NavController navController = Navigation.findNavController(frag_container);

            @Override
            public void onItemSelected(int i) {

                switch (i) {
                    case (R.id.bottom_menu):
                        drawerLayout.open();
                        bottomNavigationView.setItemSelected(R.id.bottom_menu, false, false);
                        break;
                    case (R.id.bottom_menu_conf):
                        frag_listado.elementLoaded = "conf";
                        navController.navigate(R.id.frag_listado);
                        break;
                    case (R.id.bottom_menu_videos): //Online
                        if (!Utils.isConnection(getApplicationContext())){break;}
                        frag_listado.elementLoaded = "video_conf";
                        navController.navigate(R.id.frag_listado);
                        break;
                    case (R.id.bottom_menu_books)://Online
                        if (!Utils.isConnection(getApplicationContext())){break;}
                        frag_listado.elementLoaded = "video_book";
                        navController.navigate(R.id.frag_listado);
                        break;
                    case (R.id.bottom_menu_citas):
                        frag_listado.elementLoaded = "frases";
                        navController.navigate(R.id.frag_home);
                        break;

                }

            }

        });


    } // FIN onCreate



    //Método que resuelve las callbacks enviados por llamadas de intents
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    //Lectura de código QR:
        if (QRManager.Request_Code){
           IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
           if (intentResult != null){
                ProcesarQRCode(intentResult);
           }else{
               Toast.makeText(this, "Error al leer el código QR", Toast.LENGTH_SHORT).show();
           }
            QRManager.Request_Code = false; //Reinicio de la variable controladora
        }
    }

    //Pedir los permisos de almacenamiento
    private void obtenerPermisos() {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {

            Dexter.withContext(this)
                    .withPermissions(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ).withListener(new MultiplePermissionsListener() {
                        @Override
                        public void onPermissionsChecked(MultiplePermissionsReport report) {

                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

                        }
                    }).check();

        } else { //se tiene los permisos de almacenamiento

            Utils.CrearDirectoriosRepo(MainActivity.this);

        }
    }

  /*  @Override  //Deshabilitando el boton backpres
    public void onBackPressed() {

    }*/


    //estableciendo un menú principal
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menuprincipal, menu);
        return true;
    } //onCreateOptionsMenu


    //handle eventos del menú principal
    @SuppressLint("Range")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case (R.id.main_menu_shared_app): //Online
                if (!Utils.isConnection(getApplicationContext())){break;}
              QRManager.ShowQRDialog(this,"https://play.google.com/store/apps/details?id=com.ypg.neville","Compartir App Neville",null );
                break;
            case (R.id.main_menu_leerQR):
                QRManager.launch_QRRead();
                break;
            case (R.id.main_menu_myinfo):
                deselecItemBottom(); //desselecciona los iconos en la bottom bar
                NavController navController = Navigation.findNavController(frag_container);
                navController.navigate(R.id.frag_list_info);

                break;
            case (R.id.main_menu_setup):
                deselecItemBottom();
                NavController navController1 = Navigation.findNavController(frag_container);
                navController1.navigate(R.id.fragSetting);

                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    } //onOptionsItemSelected


    /**
     * Establece el color del icono del  favorito
     * @param fav_state
     */
    public void setFavColor(String fav_state) {
        if (Objects.equals(fav_state, "1")) {
            ic_toolsBar_fav.setColorFilter(getApplicationContext().getResources().getColor(R.color.fav_active, null));
            animate(ic_toolsBar_fav); //Animacion
        } else {
            ic_toolsBar_fav.setColorFilter(getApplicationContext().getResources().getColor(R.color.fav_inactive, null));
        }

    }

    //Anima un icono
    private void animate(View view) {
        ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat("scaleX", 1.3f),
                PropertyValuesHolder.ofFloat("scaleY", 1.3f));
        scaleDown.setDuration(300);
        scaleDown.setAutoCancel(false);
        scaleDown.setRepeatCount(3);
        scaleDown.setRepeatMode(ObjectAnimator.REVERSE);

        scaleDown.start();
    }

    //auxiliar: deselecciona todos los items en el bottomMenu
    private void deselecItemBottom() {
        bottomNavigationView.setItemSelected(R.id.bottom_menu_conf, false);
        bottomNavigationView.setItemSelected(R.id.bottom_menu_books, false);
        bottomNavigationView.setItemSelected(R.id.bottom_menu_citas, false);
        bottomNavigationView.setItemSelected(R.id.bottom_menu_videos, false);
    }


    /**
     * Procesa el código QR leído
     @param intentResult  Objeto a procesar
     */
    private void ProcesarQRCode(IntentResult intentResult){
        //Comprobando que no sea una cadena vacía:
        String result = intentResult.getContents(); //Aquí esta el contenido

        if (result.trim().isEmpty()){
            Toast.makeText(this, "No se puede importar un texto vacío", Toast.LENGTH_SHORT).show();
            QRManager.Request_Code = false;
            return;
        }

        long res; //resultado de las operaciones de insert a la BD

        //Pasando a procesamiento

            //Separando campos( [f, texto, autor, fuente] [a, title, apunte] ): f de frase, a de apunte

            String[] temp = result.split("&&");

            if (temp[0].contains("f")){ //Importando frases

                    ContentValues contentValues = new ContentValues();
                    contentValues.put("frase", temp[1]);
                    contentValues.put("autor", temp[2]);
                    contentValues.put("fuente", temp[3]);
                    UiModalWindows.Add_New_frase(this, contentValues);

            } else if (temp[0].contains("a")){

                ContentValues contentValues = new ContentValues();
                contentValues.put("title", temp[1]);
                contentValues.put("apunte", temp[2]);
                UiModalWindows.ApunteManager(this,"",contentValues,false);
            }

}

    /**
     * Establece el color de las barras de la app
     * @param color valor de color como R.color...
     */
    public void AuxSetColorBar(int color){
    Drawable background = toolbar.getBackground();
    if (color != 0){
        background.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_ATOP));
        background = bottomNavigationView.getBackground();background.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_ATOP));
    }
}



} //class