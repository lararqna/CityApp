package com.example.cityapp.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.cityapp.R
import com.example.cityapp.ui.theme.AppTheme
import com.example.cityapp.ui.theme.CityAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

@Composable
fun RegisterScreen(
    navController: NavController,
    auth: FirebaseAuth?
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }

    val goToPage: (Int) -> Unit = { page -> currentPage = page }

    val handleFinalRegistration = { selectedUri: Uri? ->
        isLoading = true
        auth?.createUserWithEmailAndPassword(email, password)
            ?.addOnCompleteListener { authTask ->
                if (!authTask.isSuccessful) {
                    isLoading = false
                    val error = authTask.exception?.message ?: context.getString(R.string.error_unknown)
                    val errorMessage = context.getString(R.string.error_registration_failed, error)
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    return@addOnCompleteListener
                }

                val firebaseUser = authTask.result?.user ?: run {
                    isLoading = false; return@addOnCompleteListener
                }

                if (selectedUri != null) {
                    val storageRef = Firebase.storage.reference
                    val imageRef = storageRef.child("profile_pictures/${firebaseUser.uid}")
                    imageRef.putFile(selectedUri)
                        .continueWithTask { task ->
                            if (!task.isSuccessful) {
                                task.exception?.let { throw it }
                            }
                            imageRef.downloadUrl
                        }
                        .addOnCompleteListener { urlTask ->
                            val downloadUrl = if (urlTask.isSuccessful) urlTask.result.toString() else ""
                            saveUserDataToFirestore(firebaseUser, downloadUrl, navController, context, firstName, lastName, email, birthDate) { isLoading = false }
                        }
                } else {
                    saveUserDataToFirestore(firebaseUser, "", navController, context, firstName, lastName, email, birthDate) { isLoading = false }
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(120.dp))

            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = stringResource(R.string.register),
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.welcome_to),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.adventure_begins_register),
                color = AppTheme.extendedColors.inactiveText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(30.dp))


            Crossfade(targetState = currentPage, label = "ToggleToBackArrow", animationSpec = tween(300)) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(43.dp)
                ) {
                    if (page == 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    AppTheme.extendedColors.toggleBackground,
                                    RoundedCornerShape(5.dp)
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { navController.navigate("login") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.login), color = AppTheme.extendedColors.inactiveText)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(5.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.register),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        TextButton(
                            onClick = { goToPage(currentPage - 1) },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back_to_previous_step)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.back))
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    val enter = slideInHorizontally { width -> width } + fadeIn()
                    val exit = slideOutHorizontally { width -> -width } + fadeOut()
                    enter togetherWith exit
                }, label = "page_animation"
            ) { page ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(24.dp))
                    when (page) {
                        1 -> AccountCreationPage(
                            email = email,
                            onEmailChange = { email = it },
                            password = password,
                            onPasswordChange = { password = it },
                            onNextClicked = {
                                val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                                if (isEmailValid && password.length >= 6) {
                                    goToPage(2)
                                } else {
                                    Toast.makeText(context, context.getString(R.string.info_check_email_password), Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        2 -> ProfileDetailsPage(
                            firstName = firstName,
                            onFirstNameChange = { firstName = it },
                            lastName = lastName,
                            onLastNameChange = { lastName = it },
                            birthDate = birthDate,
                            onBirthDateChange = { birthDate = it },
                            onNextClicked = {
                                if (firstName.isNotBlank() && lastName.isNotBlank()) {
                                    goToPage(3)
                                } else {
                                    Toast.makeText(context, context.getString(R.string.info_required_fields), Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        3 -> ProfilePicturePage(
                            selectedImageUri = profileImageUri,
                            onImageSelected = { profileImageUri = it },
                            isLoading = isLoading,
                            onFinishClicked = { handleFinalRegistration(profileImageUri) },
                            onSkipClicked = { handleFinalRegistration(null) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

private fun saveUserDataToFirestore(
    firebaseUser: FirebaseUser,
    profilePictureUrl: String,
    navController: NavController,
    context: Context,
    firstName: String,
    lastName: String,
    email: String,
    birthDate: String,
    onComplete: () -> Unit
) {
    val userMap = hashMapOf(
        "firstName" to firstName.trim(),
        "lastName" to lastName.trim(),
        "email" to email.trim().lowercase(),
        "birthDate" to birthDate.trim(),
        "profilePictureUrl" to profilePictureUrl
    )

    Firebase.firestore.collection("users").document(firebaseUser.uid)
        .set(userMap)
        .addOnSuccessListener {
            val welcomeMessage = context.getString(R.string.success_welcome, firstName)
            Toast.makeText(context, welcomeMessage, Toast.LENGTH_SHORT).show()
            navController.navigate("home") { popUpTo(navController.graph.startDestinationId) { inclusive = true } }
        }
        .addOnFailureListener { e ->
            val errorMessage = context.getString(R.string.error_profile_save_failed, e.message)
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        }
        .addOnCompleteListener { onComplete() }
}


@Composable
fun AccountCreationPage(
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    onNextClicked: () -> Unit
) {
    var isPasswordFocused by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(R.string.email_address)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(5.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.password)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isPasswordFocused = focusState.isFocused
                },
            shape = RoundedCornerShape(5.dp)
        )

        if (isPasswordFocused) {
            Spacer(modifier = Modifier.height(16.dp))
            PasswordRequirementHint()
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNextClicked,
            shape = RoundedCornerShape(5.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(stringResource(R.string.next), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PasswordRequirementHint() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Info",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.password_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProfileDetailsPage(
    firstName: String, onFirstNameChange: (String) -> Unit,
    lastName: String, onLastNameChange: (String) -> Unit,
    birthDate: String, onBirthDateChange: (String) -> Unit,
    onNextClicked: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.almost_done), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(value = firstName, onValueChange = onFirstNameChange, label = { Text(stringResource(R.string.first_name_required)) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(5.dp))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = lastName, onValueChange = onLastNameChange, label = { Text(stringResource(R.string.last_name_required)) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(5.dp))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = birthDate, onValueChange = onBirthDateChange, label = { Text(stringResource(R.string.birth_date_optional)) }, placeholder = { Text(stringResource(R.string.date_placeholder)) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(5.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNextClicked,
            shape = RoundedCornerShape(5.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Volgende", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfilePicturePage(
    selectedImageUri: Uri?,
    onImageSelected: (Uri?) -> Unit,
    isLoading: Boolean,
    onFinishClicked: () -> Unit,
    onSkipClicked: () -> Unit
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> onImageSelected(uri) }
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "Nog één ding!",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Voeg een profielfoto toe (optioneel).",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { imagePickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(selectedImageUri),
                    contentDescription = "Geselecteerde profielfoto",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profielfoto placeholder",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tik om een foto te kiezen",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onFinishClicked,
            enabled = !isLoading,
            shape = RoundedCornerShape(5.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(R.string.complete_registration), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onSkipClicked, enabled = !isLoading) {
            Text("Nu overslaan")
        }
    }
}

@Preview(showBackground = true, device = "spec:width=393dp,height=852dp,dpi=480")
@Composable
fun RegisterScreenPreview() {
    CityAppTheme {
        RegisterScreen(
            navController = rememberNavController(),
            auth = null
        )
    }
}
