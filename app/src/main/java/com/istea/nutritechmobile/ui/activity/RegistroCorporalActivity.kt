package com.istea.nutritechmobile.ui.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.annotation.MainThread
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.istea.nutritechmobile.R
import com.istea.nutritechmobile.data.RegistroCorporal
import com.istea.nutritechmobile.data.UserResponse
import com.istea.nutritechmobile.firebase.FirebaseAuthManager
import com.istea.nutritechmobile.firebase.FirebaseFirestoreManager
import com.istea.nutritechmobile.firebase.FirebaseStorageManager
import com.istea.nutritechmobile.helpers.CameraManager
import com.istea.nutritechmobile.helpers.UIManager
import com.istea.nutritechmobile.helpers.extensions.dateFromString
import com.istea.nutritechmobile.helpers.extensions.stringFromDate
import com.istea.nutritechmobile.model.PerfilPacienteRepositoryImp
import com.istea.nutritechmobile.model.RegistroCorporalRepositoryImp
import com.istea.nutritechmobile.presenter.PerfilPacientePresenterImp
import com.istea.nutritechmobile.presenter.RegistroCorporalPresenterImp
import com.istea.nutritechmobile.presenter.interfaces.IPerfilPacientePresenter
import com.istea.nutritechmobile.presenter.interfaces.IRegistroCorporalPresenter
import com.istea.nutritechmobile.ui.interfaces.IPerfilPacienteView
import com.istea.nutritechmobile.ui.interfaces.IRegistroCorporalView
import kotlinx.coroutines.*
import java.util.*
private const val campoNoAsignado = "No asignado"
class RegistroCorporalActivity : AppCompatActivity(), IRegistroCorporalView, IPerfilPacienteView {
    private lateinit var toolbar: Toolbar
    private lateinit var etPeso: EditText
    private lateinit var etCintura: EditText
    private lateinit var imgEstadoFisico: ImageView
    private lateinit var btnTakePhoto: ImageButton
    private lateinit var btnDeletePhoto: ImageButton
    private lateinit var btnGuardar: Button
    private lateinit var hiddenFileUpload: TextView
    private lateinit var hiddenImageName: TextView
    private lateinit var txtPhotoAddThumbnail: TextView
    private lateinit var imgPhotoAddThumbnail: ImageView
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etMail: EditText
    private lateinit var etFechaNacimiento: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etAltura: EditText
    private lateinit var etMedidaCintura: EditText
    private lateinit var etTipoAlimentacion: EditText
    private var calendar: GregorianCalendar = GregorianCalendar()

    private val perfilPresenter: IPerfilPacientePresenter by lazy {
        PerfilPacientePresenterImp(this, PerfilPacienteRepositoryImp(FirebaseFirestoreManager(this)))
    }

    private val firebaseStorageManager: FirebaseStorageManager by lazy {
        FirebaseStorageManager(this, FirebaseAuthManager())
    }

    private val camera: CameraManager by lazy {
        CameraManager(this, imgEstadoFisico, hiddenFileUpload, hiddenImageName)
    }

    private val presenter: IRegistroCorporalPresenter by lazy {
        RegistroCorporalPresenterImp(
            this,
            RegistroCorporalRepositoryImp(FirebaseFirestoreManager(this)),
            firebaseStorageManager,
            camera
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch(Dispatchers.Main) {
            perfilPresenter.getPaciente()
        }
        setContentView(R.layout.activity_registro_corporal)
        setupUI()
    }


    private fun setupUI() {
        etPeso = findViewById(R.id.etPeso)
        etCintura = findViewById(R.id.etCintura)
        imgEstadoFisico = findViewById(R.id.imgEstadoFisico)
        txtPhotoAddThumbnail = findViewById(R.id.txtPhotoAddThumbnail)
        imgPhotoAddThumbnail = findViewById(R.id.imgPhotoAddThumbnail)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnDeletePhoto = findViewById(R.id.btnDeletePhoto)
        btnGuardar = findViewById(R.id.btnGuardar)
        hiddenFileUpload = findViewById(R.id.hiddenFileUpload)
        hiddenImageName = findViewById(R.id.hiddenImageName)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        btnGuardar.isEnabled = false

        setupToolbar()
        setupBottomNavigationBar(bottomNavigationView)
        enableDefaultPhotoThumbnail()
        validateForm()
        bindEvents()
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        //Hiding default app icon
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun bindEvents() {

        btnTakePhoto.setOnClickListener {
            camera.takePhoto()
            lifecycleScope.launch(Dispatchers.Main) {
                delay(1000)
                disableDefaultPhotoThumbnail()
            }

        }

        btnDeletePhoto.setOnClickListener {
            camera.cleanPhoto()
            hiddenFileUpload.text = ""
            enableDefaultPhotoThumbnail()
        }

        btnGuardar.setOnClickListener {
            submitForm()
        }

        hiddenFileUpload.addTextChangedListener {
            validateForm()
        }

        etPeso.addTextChangedListener {
            validateForm()
        }

        etCintura.addTextChangedListener {
            validateForm()
        }

    }

    private fun buildCorporalRegistry(): RegistroCorporal {
        return RegistroCorporal(
            hiddenImageName.text.toString(),
            hiddenFileUpload.text.toString(),
            etPeso.text.toString().toFloatOrNull() ?: 0f,
            etCintura.text.toString().toFloatOrNull() ?: 0f,
        )
    }

    private fun buildPacienteFromForm(): UserResponse {
        val updatedUser = UserResponse()
        updatedUser.Nombre = etNombre.text.toString()
        updatedUser.Apellido = etApellido.text.toString()
        updatedUser.Telefono = etTelefono.text.toString()
        updatedUser.Email = etMail.text.toString()
        updatedUser.TipoAlimentacion = etTipoAlimentacion.text.toString()
        updatedUser.FechaNacimiento = setFechaNacimiento()

        updatedUser.Altura =
            if (etAltura.text.toString()
                    .isNotEmpty() && etAltura.text.toString() != campoNoAsignado
            ) etAltura.text.toString()
                .toFloat() else 0f
        updatedUser.Peso = etPeso.text.toString().toFloatOrNull() ?: 0f
        updatedUser.MedidaCintura = etCintura.text.toString().toFloatOrNull() ?: 0f

        return updatedUser;
    }

    private fun submitForm() {
        val registro = buildCorporalRegistry()
        updatePacienteInfo()
        val user = FirebaseAuthManager().getAuthEmail()
        presenter.addCorporalRegistry(user, registro)
        resetForm()
    }

    private fun enableDefaultPhotoThumbnail() {
        imgEstadoFisico.isVisible = false
        imgPhotoAddThumbnail.isVisible = true
        txtPhotoAddThumbnail.isVisible = true
    }

    private fun disableDefaultPhotoThumbnail() {
        imgEstadoFisico.isVisible = true
        imgPhotoAddThumbnail.isVisible = false
        txtPhotoAddThumbnail.isVisible = false
    }


    private fun validateForm() {
        deactivateSubmitButton()

        if (etCintura.text.isNotEmpty() && etPeso.text.isNotEmpty()) {
            if (hiddenFileUpload.text.isNotEmpty()) {
                activateSubmitButton()
            }
        }

    }

    private fun activateSubmitButton() {
        btnGuardar.isEnabled = true
    }

    private fun deactivateSubmitButton() {
        btnGuardar.isEnabled = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        camera.activityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        camera.requestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun setupBottomNavigationBar(bottomNavigationView: BottomNavigationView) {
        val mOnNavigationItemSelectedListener =
            BottomNavigationView.OnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.registro_diario -> {
                        goToDailyRegistryView()
                        return@OnNavigationItemSelectedListener true
                    }
                    R.id.recetas -> {
                        goToRecipesView()
                        return@OnNavigationItemSelectedListener true

                    }
                    R.id.progreso -> {
                        goToProgressView()
                        return@OnNavigationItemSelectedListener true

                    }
                    R.id.info_personal -> {
                        goToProfileView()
                        return@OnNavigationItemSelectedListener true

                    }
                }
                false
            }
        bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }

    override fun goToDailyRegistryView() {
        Intent(this@RegistroCorporalActivity, DailyRegistryActivity::class.java).apply {
            startActivity(this)
        }
    }

    override fun goToRecipesView() {
        showInProgressMessage()
    }


    private fun resetForm() {
        finish();
        overridePendingTransition(0, 0);
        Intent(this@RegistroCorporalActivity, this::class.java).apply {
            startActivity(this)
        }
        overridePendingTransition(0, 0);
    }

    override fun goToProgressView() {
        resetForm()
    }

    override fun goToProfileView() {
        Intent(this@RegistroCorporalActivity, PerfilPacienteActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(this)
        }
    }

    override fun showInProgressMessage() {
        UIManager.showMessageShort(this, "Función en desarrollo")
    }

    override fun showPacienteInfo(paciente: UserResponse) {
        etNombre.setText(if (paciente.Nombre.isNotEmpty()) paciente.Nombre else campoNoAsignado)
        etApellido.setText(if (paciente.Apellido.isNotEmpty()) paciente.Apellido else campoNoAsignado)
        etMail.setText(if (paciente.Email.isNotEmpty()) paciente.Email else campoNoAsignado)
        etFechaNacimiento.setText(getFechaNacimiento(paciente.FechaNacimiento))
        etTelefono.setText(if (paciente.Telefono.isNotEmpty()) paciente.Telefono else campoNoAsignado)
        etAltura.setText(if (paciente.Altura != null && paciente.Altura!! > 0) paciente.Altura.toString() else campoNoAsignado)
        etPeso.setText(if (paciente.Peso != null && paciente.Peso!! > 0) paciente.Peso.toString() else campoNoAsignado)
        etMedidaCintura.setText(if (paciente.MedidaCintura != null && paciente.MedidaCintura!! > 0) paciente.MedidaCintura.toString() else campoNoAsignado)
        etTipoAlimentacion.setText(if (paciente.TipoAlimentacion.isNotEmpty()) paciente.TipoAlimentacion else campoNoAsignado)
    }

    override fun updatePacienteInfo() {
        val paciente = buildPacienteFromForm()

        lifecycleScope.launch(Dispatchers.Main) {
            perfilPresenter.updatePaciente(paciente)
        }
    }

    private fun getFechaNacimiento(fechNac: Timestamp?): String {
        return if (fechNac != null) {
            calendar.timeInMillis = fechNac.seconds * 1000
            val fecha = Date(calendar.timeInMillis)
            fecha.stringFromDate()
        } else {
            campoNoAsignado
        }
    }

    private fun setFechaNacimiento(): Timestamp? {
        if (etFechaNacimiento.text.toString() != campoNoAsignado) {
            val fechaObtenida = etFechaNacimiento.text.toString().dateFromString()

            if (fechaObtenida != null) {
                return Timestamp(fechaObtenida)
            }
        }

        return null
    }

    override fun goBackToLogin() {
        TODO("Not yet implemented")
    }

    override fun goBackToMain() {
        TODO("Not yet implemented")
    }

}