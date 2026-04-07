package com.example.communityapp.ui.posts

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.communityapp.data.model.Post
import com.example.communityapp.data.model.PostStatus
import com.example.communityapp.databinding.FragmentCreatePostBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class CreatePostFragment : Fragment() {
    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PostsViewModel by activityViewModels()

    private var localImagePath: String? = null
    private var cameraImageUri: Uri? = null   // URI for the photo the camera will write to
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private var fusedLocationClient: FusedLocationProviderClient? = null

    // ─── Launchers ────────────────────────────────────────────────────────────

    /** Gallery picker — no permission needed on API 29+ */
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { copyImageToInternalStorage(it) }
    }

    /** Camera — takes a photo and writes it to cameraImageUri */
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { showImagePreview(File(it.path!!)) }
        }
    }

    /** Storage permission for gallery on Android 9 and below */
    private val galleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) galleryLauncher.launch("image/*")
        else Toast.makeText(requireContext(), "Storage permission denied", Toast.LENGTH_SHORT).show()
    }

    /** Camera permission */
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    /** Location permission */
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchLocation()
        else {
            binding.tvLocationStatus.text = "Location permission denied."
            binding.btnGetLocation.isEnabled = true
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val playOk = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(requireContext()) == ConnectionResult.SUCCESS
        if (playOk) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        } else {
            binding.btnGetLocation.isEnabled = false
            binding.tvLocationStatus.text = "Location unavailable on this device"
        }

        binding.btnSelectImage.setOnClickListener { showImageSourceDialog() }

        binding.btnGetLocation.setOnClickListener {
            if (fusedLocationClient == null) {
                Toast.makeText(requireContext(), "Location services not available", Toast.LENGTH_SHORT).show()
            } else {
                checkLocationPermissionAndFetch()
            }
        }

        binding.btnSubmit.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            if (title.isBlank()) { binding.etTitle.error = "Required"; return@setOnClickListener }
            if (description.isBlank()) { binding.etDescription.error = "Required"; return@setOnClickListener }

            val locationText = binding.tvLocationStatus.text.toString().let {
                if (it.startsWith("Location:")) it else ""
            }

            val post = Post(
                title = title,
                description = description,
                imageUrl = localImagePath ?: "",
                location = locationText,
                latitude = currentLatitude,
                longitude = currentLongitude,
                authorId = viewModel.currentUser?.id ?: "",
                authorName = viewModel.currentUser?.displayName ?: "Anonymous",
                status = PostStatus.PENDING.name,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            viewModel.createPost(post, null)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PostsUiState.Loading -> {
                    binding.btnSubmit.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is PostsUiState.PostCreated -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Post created!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                is PostsUiState.Error -> {
                    binding.btnSubmit.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    binding.btnSubmit.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    // ─── Image source dialog ──────────────────────────────────────────────────

    private fun showImageSourceDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Image")
            .setItems(arrayOf("Take a Photo", "Choose from Gallery")) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> checkGalleryPermissionAndPick()
                }
            }
            .show()
    }

    // ─── Camera ───────────────────────────────────────────────────────────────

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> launchCamera()
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(requireContext(), "Camera access is needed to take a photo.", Toast.LENGTH_LONG).show()
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        // Create a file in internal storage for the camera to write into
        val photoFile = File(
            requireContext().filesDir.also { File(it, "images").mkdirs() },
            "images/${UUID.randomUUID()}.jpg"
        )
        // FileProvider exposes the private file path to the camera app securely
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        cameraImageUri = uri
        // Store path so we can display it after the camera returns
        localImagePath = photoFile.absolutePath
        cameraLauncher.launch(uri)
    }

    // ─── Gallery ──────────────────────────────────────────────────────────────

    private fun checkGalleryPermissionAndPick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No permission needed on API 29+
            galleryLauncher.launch("image/*")
        } else {
            val perm = Manifest.permission.READ_EXTERNAL_STORAGE
            when {
                ContextCompat.checkSelfPermission(requireContext(), perm)
                        == PackageManager.PERMISSION_GRANTED -> galleryLauncher.launch("image/*")
                shouldShowRequestPermissionRationale(perm) -> {
                    Toast.makeText(requireContext(), "Storage access is needed to pick an image.", Toast.LENGTH_LONG).show()
                    galleryPermissionLauncher.launch(perm)
                }
                else -> galleryPermissionLauncher.launch(perm)
            }
        }
    }

    private fun copyImageToInternalStorage(uri: Uri) {
        try {
            val file = File(
                requireContext().filesDir.also { File(it, "images").mkdirs() },
                "images/${UUID.randomUUID()}.jpg"
            )
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            localImagePath = file.absolutePath
            showImagePreview(file)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImagePreview(file: File) {
        Glide.with(this).load(file).centerCrop().into(binding.ivSelectedImage)
        binding.ivSelectedImage.visibility = View.VISIBLE
        binding.btnSelectImage.text = "Change Image"
    }

    // ─── Location ─────────────────────────────────────────────────────────────

    private fun checkLocationPermissionAndFetch() {
        val fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            fetchLocation()
        } else {
            binding.tvLocationStatus.text = "Requesting location permission…"
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        val client = fusedLocationClient ?: return
        binding.btnGetLocation.isEnabled = false
        binding.tvLocationStatus.text = "Getting location…"
        client.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) onLocationReceived(location.latitude, location.longitude)
                else requestFreshLocation()
            }
            .addOnFailureListener {
                binding.tvLocationStatus.text = "Could not get location. Try again."
                binding.btnGetLocation.isEnabled = true
            }
    }

    @SuppressLint("MissingPermission")
    private fun requestFreshLocation() {
        val client = fusedLocationClient ?: return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(2000)
            .setMaxUpdates(1)
            .build()
        client.requestLocationUpdates(request, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                client.removeLocationUpdates(this)
                val loc = result.lastLocation
                if (loc != null) onLocationReceived(loc.latitude, loc.longitude)
                else {
                    binding.tvLocationStatus.text = "Could not get location. Try again."
                    binding.btnGetLocation.isEnabled = true
                }
            }
        }, Looper.getMainLooper())
    }

    private fun onLocationReceived(lat: Double, lng: Double) {
        currentLatitude = lat
        currentLongitude = lng
        binding.tvLocationStatus.text = "Location: %.5f, %.5f".format(lat, lng)
        binding.btnGetLocation.isEnabled = true
        binding.btnGetLocation.text = "Update Location"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}