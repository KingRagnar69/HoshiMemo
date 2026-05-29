package com.ragdev69.hoshimemo.ui.profile

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.ragdev69.hoshimemo.R
import com.ragdev69.hoshimemo.data.model.UserProfile
import com.ragdev69.hoshimemo.ui.theme.LocalHoshiColors
import com.ragdev69.hoshimemo.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel()) {
    val colors        = LocalHoshiColors.current
    val isLoggedIn   by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val profile      by viewModel.profile.collectAsStateWithLifecycle()
    val isLoading    by viewModel.isLoading.collectAsStateWithLifecycle()
    val context       = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.message.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Google Sign-In launcher
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { viewModel.signInWithGoogle(it) }
            } catch (e: ApiException) {
                // handled by viewModel message
            }
        }
    }

    Scaffold(
        containerColor = colors.background,
        snackbarHost   = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = isLoggedIn,
                transitionSpec = {
                    fadeIn(animationSpec = androidx.compose.animation.core.tween(400)) togetherWith
                            fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
                },
                label = "auth_state"
            ) { loggedIn ->
                if (loggedIn) {
                    ProfileContent(
                        profile   = profile,
                        isLoading = isLoading,
                        colors    = colors,
                        onSave    = { viewModel.saveProfile(it) },
                        onSignOut = { viewModel.signOut() }
                    )
                } else {
                    AuthScreen(
                        isLoading = isLoading,
                        colors    = colors,
                        onSignIn  = { email, pass -> viewModel.signIn(email, pass) },
                        onCreate  = { email, pass, name -> viewModel.createAccount(email, pass, name) },
                        onGoogleSignIn = {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(context.getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build()
                            val client = GoogleSignIn.getClient(context, gso)
                            client.signOut() // clear any cached account so picker always shows
                            googleLauncher.launch(client.signInIntent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AuthScreen(
    isLoading: Boolean,
    colors: com.ragdev69.hoshimemo.ui.theme.HoshiColors,
    onSignIn: (String, String) -> Unit,
    onCreate: (String, String, String) -> Unit,
    onGoogleSignIn: () -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var name     by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(colors = listOf(colors.primary, colors.accent))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("⭐", fontSize = 40.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Hoshi Memo",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold, color = colors.text
            )
        )
        Text(
            if (isSignUp) "Create your account" else "Welcome back!",
            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
        )

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(visible = isSignUp) {
            Column {
                HoshiTextField(
                    value = name, onValueChange = { name = it },
                    label = "Your Name", icon = Icons.Filled.Person, colors = colors
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        HoshiTextField(
            value = email, onValueChange = { email = it },
            label = "Email", icon = Icons.Filled.Email, colors = colors
        )
        Spacer(modifier = Modifier.height(12.dp))

        HoshiTextField(
            value = password, onValueChange = { password = it },
            label = "Password", icon = Icons.Filled.Lock, colors = colors,
            isPassword = true, showPassword = showPass,
            onTogglePassword = { showPass = !showPass }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Email/Password button
        Button(
            onClick = {
                if (isSignUp) onCreate(email, password, name)
                else onSignIn(email, password)
            },
            enabled  = email.isNotBlank() && password.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = colors.primary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White, strokeWidth = 2.dp
                )
            } else {
                Text(
                    if (isSignUp) "Create Account" else "Sign In",
                    fontWeight = FontWeight.Bold, color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Divider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = colors.border)
            Text(
                "  or  ",
                style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = colors.border)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Google Sign-In button
        OutlinedButton(
            onClick  = onGoogleSignIn,
            enabled  = !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = colors.text),
            border   = BorderStroke(1.dp, colors.border)
        ) {
            Text("G", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF4285F4))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Continue with Google", fontWeight = FontWeight.Medium, color = colors.text)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { isSignUp = !isSignUp }) {
            Text(
                if (isSignUp) "Already have an account? Sign In"
                else "New here? Create an account",
                color = colors.primary
            )
        }
    }
}

@Composable
fun ProfileContent(
    profile: UserProfile?,
    isLoading: Boolean,
    colors: com.ragdev69.hoshimemo.ui.theme.HoshiColors,
    onSave: (UserProfile) -> Unit,
    onSignOut: () -> Unit
) {
    var name    by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var age     by remember(profile) { mutableStateOf(profile?.age ?: "") }
    var bio     by remember(profile) { mutableStateOf(profile?.bio ?: "") }
    val email   = profile?.email ?: ""
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "H"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(52.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(colors = listOf(colors.primary, colors.accent))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = initial,
                style = MaterialTheme.typography.displayMedium.copy(
                    color = Color.White, fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text  = name.ifEmpty { "Hoshi User" },
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold, color = colors.text
            )
        )
        Text(
            text  = email,
            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "PROFILE INFO",
            style = MaterialTheme.typography.labelSmall.copy(
                color = colors.primary, letterSpacing = 1.5.sp
            ),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        HoshiTextField(
            value = name, onValueChange = { name = it },
            label = "Display Name", icon = Icons.Filled.Person, colors = colors
        )
        Spacer(modifier = Modifier.height(12.dp))

        HoshiTextField(
            value = age, onValueChange = { age = it },
            label = "Age", icon = Icons.Filled.Cake, colors = colors
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value         = bio,
            onValueChange = { bio = it },
            label         = { Text("Bio", color = colors.textSecondary) },
            modifier      = Modifier.fillMaxWidth().height(120.dp),
            shape         = RoundedCornerShape(16.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = colors.primary,
                unfocusedBorderColor = colors.border,
                focusedTextColor     = colors.text,
                unfocusedTextColor   = colors.text,
                cursorColor          = colors.primary
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onSave(UserProfile(
                    uid = profile?.uid ?: "", name = name,
                    email = email, age = age, bio = bio, avatarInitial = initial
                ))
            },
            enabled  = !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = colors.primary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White, strokeWidth = 2.dp
                )
            } else {
                Text("Save Profile", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick  = onSignOut,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
            border   = BorderStroke(1.dp, colors.accent)
        ) {
            Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun HoshiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colors: com.ragdev69.hoshimemo.ui.theme.HoshiColors,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: () -> Unit = {}
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label, color = colors.textSecondary) },
        leadingIcon   = { Icon(icon, contentDescription = null, tint = colors.textSecondary) },
        trailingIcon  = if (isPassword) ({
            IconButton(onClick = onTogglePassword) {
                Icon(
                    imageVector        = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = null,
                    tint               = colors.textSecondary
                )
            }
        }) else null,
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation()
        else VisualTransformation.None,
        singleLine = true,
        modifier   = Modifier.fillMaxWidth(),
        shape      = RoundedCornerShape(16.dp),
        colors     = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = colors.primary,
            unfocusedBorderColor = colors.border,
            focusedTextColor     = colors.text,
            unfocusedTextColor   = colors.text,
            cursorColor          = colors.primary
        )
    )
}