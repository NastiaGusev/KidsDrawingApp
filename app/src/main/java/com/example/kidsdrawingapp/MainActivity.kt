package com.example.kidsdrawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.core.view.get
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar


class MainActivity : AppCompatActivity() {
    private var drawingView: DrawingView? = null
    private var mainIMGBrushSize: ImageButton? = null
    private var currentPaintIMB: ImageButton? = null
    private var mainLAYColors: LinearLayout? = null
    private var mainIMGImagePicker: ImageButton? = null
    private var mainBackground: ImageView? = null

    private val cameraResultLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Permission granted!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_LONG).show()
            }
        }

    private val cameraAndLocationResultLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        )
        { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
                    when (permissionName) {
                        Manifest.permission.ACCESS_FINE_LOCATION -> {
                            Toast.makeText(
                                this,
                                "Permission fine location granted!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        Manifest.permission.CAMERA -> {
                            Toast.makeText(this, "Permission camera granted!", Toast.LENGTH_LONG)
                                .show()
                        }
                        else -> {
                            Toast.makeText(
                                this,
                                "Permission coarse location granted!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    when (permissionName) {
                        Manifest.permission.ACCESS_FINE_LOCATION -> {
                            Toast.makeText(
                                this,
                                "Permission fine location denied!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        Manifest.permission.CAMERA -> {
                            Toast.makeText(this, "Permission camera denied!", Toast.LENGTH_LONG)
                                .show()
                        }
                        else -> {
                            Toast.makeText(
                                this,
                                "Permission coarse location denied!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }

        }

    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
                    Toast.makeText(
                        this@MainActivity,
                        "Permission granted now you can read the storage files",
                        Toast.LENGTH_LONG
                    ).show()

                    //Start intent to go to the gallery which is outside our app
                    val pickIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    //Launcher to get picked photo to the app
                    openGalleryLauncher.launch(pickIntent)

                } else {
                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this@MainActivity,
                            "Permission denied",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                mainBackground?.setImageURI(result.data?.data)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.main_DrawingView)
        drawingView?.setSizeForBrush(20.toFloat())
        mainIMGBrushSize = findViewById(R.id.main_IMG_BrushSize)
        mainIMGBrushSize?.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        mainIMGImagePicker = findViewById(R.id.main_IMG_ImagePicker)
        mainBackground = findViewById(R.id.main_background)
        mainIMGImagePicker?.setOnClickListener {
            requestStoragePermission()
        }

        mainLAYColors = findViewById(R.id.main_LAY_Colors)
        currentPaintIMB = mainLAYColors!![0] as ImageButton
        currentPaintIMB!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            showRationalDialog(
                "Kids Drawing App", "Kids Drawing App needs to access your " +
                        "external storage for uploading images to assign background images."
            )
        } else {
            requestPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    /**
     * Function for creating dialog for choosing brush size
     */
    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size")

        val smallButton: ImageButton = brushDialog.findViewById(R.id.dialog_smallBrush)
        smallButton.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val mediumButton: ImageButton = brushDialog.findViewById(R.id.dialog_mediumBrush)
        mediumButton.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val largeButton: ImageButton = brushDialog.findViewById(R.id.dialog_largeBrush)
        largeButton.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    fun paintClicked(view: View) {
        if (view != currentPaintIMB) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()

            drawingView?.setColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )
            currentPaintIMB?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )
            currentPaintIMB = imageButton
        }

    }

    /**
     * Shows rationale dialog for displaying why the app needs permission
     * Only shown if the user has denied the permission request previously
     */
    private fun showRationalDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message).setPositiveButton("Cancel") { dialog, _ -> dialog.dismiss() }

        builder.create().show()
    }


    /**
     * Function for creating short progress dialog for quick actions
     */
    private fun customProgressDialogFunction() {
        val customProgressDialog = Dialog(this)
        customProgressDialog.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog.show()
    }

    /**
     * Function for creating a custom dialog using built XML
     */
    private fun customDialogFunction() {
        val customDialog = Dialog(this)
        customDialog.setContentView(R.layout.dialog_custom)
        val submit: TextView = customDialog.findViewById(R.id.dialog_submit)
        val cancel: TextView = customDialog.findViewById(R.id.dialog_cancel)

        submit.setOnClickListener {
            Toast.makeText(applicationContext, "Clicked submit", Toast.LENGTH_LONG).show()
            customDialog.dismiss()
        }

        cancel.setOnClickListener {
            Toast.makeText(applicationContext, "Clicked cancel", Toast.LENGTH_LONG).show()
            customDialog.dismiss()
        }
        customDialog.show()
    }

    /**
     * Function for creating Alert Dialog
     */
    private fun alertDialogFunction() {
        val builder = AlertDialog.Builder(this)
        builder
            .setTitle("Alert")
            .setMessage("This is alert dialog. Which is used to show alert messages.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Yes") { dialogInterface, _ ->
                Toast.makeText(applicationContext, "Clicked Yes", Toast.LENGTH_LONG).show()
                dialogInterface.dismiss()
            }
            .setNeutralButton("Cancel") { dialogInterface, _ ->
                Toast.makeText(
                    applicationContext,
                    "Clicked Cancel\n operation cancel",
                    Toast.LENGTH_LONG
                ).show()
                dialogInterface.dismiss()
            }

            .setNegativeButton("No") { dialogInterface, _ ->
                Toast.makeText(applicationContext, "Clicked No", Toast.LENGTH_LONG).show()
                dialogInterface.dismiss()
            }

        val alertDialog: AlertDialog = builder.create()
        //Doesn't let the user dismiss dialog by clicking outside the dialog
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    /**
     * Function for launching permission launcher
     */
    private fun launchCameraLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldShowRequestPermissionRationale(
                Manifest.permission.CAMERA
            )
        ) {
            showRationalDialog(
                "Permission Demo requires camera access",
                "Camera cannot be used because access is denied"
            )
        } else {

            cameraAndLocationResultLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}