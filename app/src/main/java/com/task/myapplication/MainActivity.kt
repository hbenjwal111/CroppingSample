package com.task.myapplication

import android.Manifest.permission
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.Camera
import android.hardware.Camera.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.*


class MainActivity : AppCompatActivity(), SurfaceHolder.Callback, PictureCallback {
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var camera: Camera
    private lateinit var surfaceView: SurfaceView
    var mDist = 0f
    private var cameraId = 0
    private val neededPermissions = arrayOf(permission.CAMERA, permission.WRITE_EXTERNAL_STORAGE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        surfaceView = findViewById(R.id.surfaceView)
        val result = checkPermission()
        if (result) {
            setupSurfaceHolder()
        }
    }

    override fun onResume() {
        super.onResume()


    }

    private fun checkPermission(): Boolean {
        val currentAPIVersion = Build.VERSION.SDK_INT
        if (currentAPIVersion >= Build.VERSION_CODES.M) {
            val permissionsNotGranted = ArrayList<String>()
            for (permission in neededPermissions) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionsNotGranted.add(permission)
                }
            }
            if (permissionsNotGranted.size > 0) {
                var shouldShowAlert = false
                for (permission in permissionsNotGranted) {
                    shouldShowAlert =
                        ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
                }
                if (shouldShowAlert) {
                    showPermissionAlert(permissionsNotGranted.toTypedArray())
                } else {
                    requestPermissions(permissionsNotGranted.toTypedArray())
                }
                return false
            }
        }
        return true
    }

    private fun showPermissionAlert(permissions: Array<String>) {
        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setCancelable(true)
        alertBuilder.setTitle(R.string.permission_required)
        alertBuilder.setMessage(R.string.permission_message)
        alertBuilder.setPositiveButton(
            "YES"
        ) { _, _ -> requestPermissions(permissions) }
        val alert = alertBuilder.create()
        alert.show()
    }

    private fun requestPermissions(permissions: Array<String>) {
        ActivityCompat.requestPermissions(this@MainActivity, permissions, REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE -> {
                for (result in grantResults) {
                    if (result == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(
                            this@MainActivity,
                            R.string.permission_warning,
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }
                }
                setViewVisibility(R.id.startBtn, View.VISIBLE)
                /*  setViewVisibility(R.id.surfaceView, View.VISIBLE)*/
                setupSurfaceHolder()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun setViewVisibility(id: Int, visibility: Int) {
        val view = findViewById<View>(id)
        if (view != null) {
            view.visibility = visibility
        }
    }

    private fun setupSurfaceHolder() {
        surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(this)
        startCamera()
        setBtnClick()
    }


    private fun setBtnClick() {
        val startBtn = findViewById<Button>(R.id.startBtn)
        startBtn?.setOnClickListener { captureImage() }
    }

    private fun captureImage() {
        camera.takePicture(null, null, this)
    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        startCamera()
    }

    private fun startCamera() {
        cameraId = findFrontFacingCamera();
        camera = Camera.open(cameraId)
        camera.setDisplayOrientation(90)
        try {
            camera.setPreviewDisplay(surfaceHolder)
            camera.startPreview()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    private fun findFrontFacingCamera(): Int {
        var cameraId = -1
        // Search for the front facing camera
        val numberOfCameras = Camera.getNumberOfCameras()
        for (i in 0 until numberOfCameras) {
            val info = CameraInfo()
            Camera.getCameraInfo(i, info)
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i
                break
            }
        }
        return cameraId
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {
        resetCamera()
    }

    private fun resetCamera() {
        if (surfaceHolder.surface == null) {
            // Return if preview surface does not exist
            return
        }
        // Stop if preview surface is already running.
        camera.stopPreview()
        try {
            // Set preview display
            camera.setPreviewDisplay(surfaceHolder)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        // Start the camera preview...
        camera.startPreview()
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
        releaseCamera()
    }

    private fun releaseCamera() {
        camera.stopPreview()
        camera.release()
    }

    override fun onPictureTaken(bytes: ByteArray, camera: Camera) {

        saveImage(bytes)

        resetCamera()
    }

    private fun saveImage(bytes: ByteArray) {
        val outStream: FileOutputStream
        var original: Bitmap? = null
        var mask: Bitmap? = null
        var result: Bitmap? = null

        try {
            val fileName = "Task_Temp"
            val file =
                getExternalFilesDir(Environment.DIRECTORY_DCIM).toString() + "/" + fileName + ".png";
            outStream = FileOutputStream(file)
            outStream.write(bytes)
            outStream.close()
            original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            original = rotateImage(original, -90F)!!
            mask = BitmapFactory.decodeResource(resources, R.drawable.img)
            result = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
            val mCanvas = Canvas(result)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            mCanvas.drawBitmap(original, null, Rect(0, 0, original.width, original.height), null)
            Log.d("height", "height- ${original.height}, width - ${original.width}")
            mCanvas.drawBitmap(mask, null, Rect(0, 0, original.width, original.height), paint)
            paint.xfermode = null
            saveImage(result, "final_result")
            // imageViewResult?.setImageBitmap(result)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            original?.recycle()
            mask?.recycle()
            result?.recycle()
        }
    }

    @Throws(IOException::class)
    private fun saveImage(bitmap: Bitmap, @NonNull name: String) {
        val saved: Boolean
        val fos: OutputStream?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver: ContentResolver = applicationContext.contentResolver
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            contentValues.put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "DCIM/${Companion.IMAGES_FOLDER_NAME}"
            )
            val imageUri: Uri? =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
             val file = File(imageUri?.path)
             if(file.exists()){
                 file.delete()
             }
            fos = imageUri?.let { resolver.openOutputStream(it) }
            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_LONG).show()
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM
            ).toString() + File.separator + Companion.IMAGES_FOLDER_NAME
            val file = File(imagesDir)
            if (!file.exists()) {
                file.mkdir()
            }
            val image = File(imagesDir, "$name.png")
            fos = FileOutputStream(image)
        }
        Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_LONG).show()
        saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos?.flush()
        fos?.close()
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Get the pointer ID
        val params: Parameters = camera.parameters
        val action = event.action
        if (event.pointerCount > 1) {
            // handle multi-touch events
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                mDist = getFingerSpacing(event)
            } else if (action == MotionEvent.ACTION_MOVE && params.isZoomSupported) {
                camera.cancelAutoFocus()
                handleZoom(event, params)
            }
        } else {
            // handle single touch events
            if (action == MotionEvent.ACTION_UP) {
                handleFocus(event, params)
            }
        }
        return true
    }

    private fun handleZoom(event: MotionEvent, params: Parameters) {
        val maxZoom = params.maxZoom
        var zoom = params.zoom
        val newDist = getFingerSpacing(event)
        if (newDist > mDist) {
            //zoom in
            if (zoom < maxZoom) zoom++
        } else if (newDist < mDist) {
            //zoom out
            if (zoom > 0) zoom--
        }
        mDist = newDist
        params.zoom = zoom
        camera.setParameters(params)
    }

   private fun handleFocus(event: MotionEvent, params: Parameters) {
        val pointerId = event.getPointerId(0)
        val pointerIndex = event.findPointerIndex(pointerId)
        // Get the pointer's current position
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)
        val supportedFocusModes = params.supportedFocusModes
        if (supportedFocusModes != null && supportedFocusModes.contains(Parameters.FOCUS_MODE_AUTO)) {
            camera.autoFocus(AutoFocusCallback { b, camera ->
                // currently set to auto-focus on single touch
            })
        }
    }

    /** Determine the space between the first two fingers  */
    private fun getFingerSpacing(event: MotionEvent): Float {
        // ...
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return Math.sqrt((x * x + y * y).toDouble()).toFloat()
    }

    companion object {
        const val REQUEST_CODE = 100
        const val IMAGES_FOLDER_NAME = "TASK_IMAGES"
    }
}