package com.example.kidsdrawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.Image
import android.media.MediaScannerConnection
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.view.get
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception


class MainActivity : AppCompatActivity() {
    private var drawingView: DrawingView? = null
    private var mainBackground: ImageView? = null

    private var mainIMGBrushSize: ImageButton? = null
    private var mainIMGImagePicker: ImageButton? = null
    private var mainIMGUndo: ImageButton? = null
    private var mainIMGSave: ImageButton? = null
    private var mainIMGColor: ImageButton? = null
    private var mainIMGErase: ImageButton? = null
    private var mainIMGShare: ImageButton? = null


    private var customProgressBar: Dialog? = null

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

        mainIMGColor = findViewById(R.id.main_IMG_Color)
        mainIMGColor?.setOnClickListener {
            showBrushColorChooserDialog()
        }

        mainIMGErase = findViewById(R.id.main_IMG_Erase)
        mainIMGErase?.setOnClickListener {
            drawingView?.setColor("#FFFFFFFF")
            mainIMGColor?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.brush_white))
        }

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

        mainIMGUndo = findViewById(R.id.main_IMG_Undo)
        mainIMGUndo?.setOnClickListener {
            drawingView?.onClickUndo()
        }

        mainIMGShare = findViewById(R.id.main_IMG_Share)
        mainIMGShare?.setOnClickListener {
            showProgressDialog()
            if (isReadStorageAllowed()) {
                lifecycleScope.launch {
                    val drawingView: FrameLayout = findViewById(R.id.main_frameLayout)
                    saveBitmapFile(getBitmapFromView(drawingView), "share")
                }
            }
        }

        mainIMGSave = findViewById(R.id.main_IMG_Save)
        mainIMGSave?.setOnClickListener {

            showProgressDialog()
            if (isReadStorageAllowed()) {
                lifecycleScope.launch {
                    val drawingView: FrameLayout = findViewById(R.id.main_frameLayout)
                    saveBitmapFile(getBitmapFromView(drawingView), "save")
                }
            }
        }

    }

    private fun cancelProgressDialog() {
        if (customProgressBar != null) {
            customProgressBar?.dismiss()
            customProgressBar = null
        }
    }

    private fun showProgressDialog() {
        customProgressBar = Dialog(this@MainActivity)
        //Set the screen content from a layout resource.
        //The resource will be inflated adding all the top level views to the screen
        customProgressBar?.setContentView(R.layout.dialog_custom_progress)
        //Start the dialog and display it on the screen
        customProgressBar?.show()
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

    /**
     * Function for creating dialog for choosing brush size
     */
    private fun showBrushColorChooserDialog() {
        val brushColorDialog = Dialog(this)
        brushColorDialog.setContentView(R.layout.dialog_brush_color)
        brushColorDialog.setTitle("Brush color")

        val blackButton: ImageButton = brushColorDialog.findViewById(R.id.dialog_black)
        blackButton.setOnClickListener {
            val colorTag = blackButton.tag.toString()
            drawingView?.setColor(colorTag)
            mainIMGColor?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.brush_black))
            brushColorDialog.dismiss()
        }

        val yellowButton: ImageButton = brushColorDialog.findViewById(R.id.dialog_yellow)
        yellowButton.setOnClickListener {
            val colorTag = yellowButton.tag.toString()
            drawingView?.setColor(colorTag)
            mainIMGColor?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.brush_yellow))
            brushColorDialog.dismiss()
        }

        val blueButton: ImageButton = brushColorDialog.findViewById(R.id.dialog_blue)
        blueButton.setOnClickListener {
            val colorTag = blueButton.tag.toString()
            drawingView?.setColor(colorTag)
            mainIMGColor?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.brush_blue))
            brushColorDialog.dismiss()
        }

        val greyButton: ImageButton = brushColorDialog.findViewById(R.id.dialog_grey)
        greyButton.setOnClickListener {
            val colorTag = greyButton.tag.toString()
            drawingView?.setColor(colorTag)
            mainIMGColor?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.brush_grey))
            brushColorDialog.dismiss()
        }

        val pinkButton: ImageButton = brushColorDialog.findViewById(R.id.dialog_pink)
        pinkButton.setOnClickListener {
            val colorTag = pinkButton.tag.toString()
            drawingView?.setColor(colorTag)
            mainIMGColor?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.brush_pink))
            brushColorDialog.dismiss()
        }

        val greenButton: ImageButton = brushColorDialog.findViewById(R.id.dialog_green)
        greenButton.setOnClickListener {
            val colorTag = greenButton.tag.toString()
            drawingView?.setColor(colorTag)
            mainIMGColor?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.brush_green))
            brushColorDialog.dismiss()
        }

        val orangeButton: ImageButton = brushColorDialog.findViewById(R.id.dialog_orange)
        orangeButton.setOnClickListener {
            val colorTag = orangeButton.tag.toString()
            drawingView?.setColor(colorTag)
            mainIMGColor?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.brush_orange))
            brushColorDialog.dismiss()
        }

        val purpleButton: ImageButton = brushColorDialog.findViewById(R.id.dialog_purple)
        purpleButton.setOnClickListener {
            val colorTag = purpleButton.tag.toString()
            drawingView?.setColor(colorTag)
            mainIMGColor?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.brush_purple))
            brushColorDialog.dismiss()
        }

        val purple2Button: ImageButton = brushColorDialog.findViewById(R.id.dialog_lightPurple)
        purple2Button.setOnClickListener {
            val colorTag = purple2Button.tag.toString()
            drawingView?.setColor(colorTag)
            mainIMGColor?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.brush_purple2))
            brushColorDialog.dismiss()
        }

        brushColorDialog.show()
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

    private fun isReadStorageAllowed(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
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
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    /**
     * Function for storing the view of the app into a bitmap
     * Bitmap is stored more easily on device
     */
    private fun getBitmapFromView(view: View): Bitmap {
        //Get the drawing from the view
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(returnedBitmap)
        //Get the background of the drawing
        val background = view.background

        if (background != null) {
            //If there is an image in the background add to canvas
            background.draw(canvas)
        } else {
            //If there is no image add white background
            canvas.drawColor(Color.WHITE)
        }
        //draw the canvas on our view
        view.draw(canvas)

        return returnedBitmap
    }

    private suspend fun saveBitmapFile(bitmap: Bitmap?, choose: String): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (bitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val file =
                        File(
                            externalCacheDir?.absoluteFile.toString()
                                    + File.separator + "KidsDrawingApp_"
                                    + System.currentTimeMillis() / 1000
                                    + ".png"
                        )
                    val fileOutputStream = FileOutputStream(file)
                    fileOutputStream.write(bytes.toByteArray())
                    fileOutputStream.close()

                    result = file.absolutePath

                    runOnUiThread {
                        cancelProgressDialog()
                        if(choose == "save"){
                            runUISaveImage(result)
                        }else if(choose == "share")
                            runUIShareImage(result)
                    }

                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun runUISaveImage (result: String){
        if (result.isNotEmpty()) {
            Toast.makeText(
                this@MainActivity,
                "File save successfully : $result",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                this@MainActivity,
                "Something went wrong saving the file!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun runUIShareImage (result: String){
        if (result.isNotEmpty()) {
            shareImage(result)
        } else {
            Toast.makeText(
                this@MainActivity,
                "Something went wrong sharing the file!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun shareImage(result: String) {
        MediaScannerConnection.scanFile(this, arrayOf(result), null) { path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }


    /**
     * for launching the function
     * showProgressDialog()
     * lifecycleScope.launch {
     *      execute("Task executed successfully")
     * }
     */
    private suspend fun execute(result: String) {
        withContext(Dispatchers.IO) {
            for (i in 1..100000) {
                Log.e("TAG", "" + i)
            }
            runOnUiThread {
                cancelProgressDialog()
                Toast.makeText(this@MainActivity, result, Toast.LENGTH_LONG).show()
            }
        }
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