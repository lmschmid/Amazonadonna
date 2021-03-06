package com.amazonadonna.view

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.util.Log
import android.content.Intent
import android.view.MotionEvent
import androidx.appcompat.app.AlertDialog
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast

import com.amazonaws.regions.Regions

import com.amazon.identity.auth.device.AuthError
import com.amazon.identity.auth.device.api.Listener
import com.amazon.identity.auth.device.api.authorization.*
import com.amazon.identity.auth.device.api.workflow.RequestContext
import com.amazonadonna.artisanOnlyViews.HomeScreenArtisan
import com.amazonadonna.artisanOnlyViews.ArtisanUpdatePassword
import com.amazonadonna.model.App
import com.amazonadonna.model.Artisan


import kotlinx.android.synthetic.main.activity_login_screen.*
import com.amazonaws.mobileconnectors.cognitoidentityprovider.*
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.NewPasswordContinuation
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.IOException

const val AUTHORITY = "com.amazonadonna.provider"
const val ACCOUNT_TYPE = "amazonadonna.com"
const val ACCOUNT = "dummyaccount3"
const val SECONDS_PER_MINUTE = 60L
const val SYNC_INTERVAL_IN_MINUTES = 60L
const val SYNC_INTERVAL = SYNC_INTERVAL_IN_MINUTES * SECONDS_PER_MINUTE

class LoginScreen : AppCompatActivity() {
    private val getArtisanUrl = App.BACKEND_BASE_URL + "/artisan/listAllForEmail"
    private var requestContext : RequestContext = RequestContext.create(this)
    private val scopes : Array<Scope> = arrayOf(ProfileScope.profile(), ProfileScope.postalCode(), ProfileScope.profile())
    private lateinit var alertDialog : AlertDialog
    var userPool = CognitoUserPool(this@LoginScreen, "us-east-2_ViMIOaCbk","4in76ncc44ufi8n1sq6m5uj7p7", "12qfl0nmg81nlft6aunvj6ec0ocejfecdau80biodpubkfuna0ee", Regions.US_EAST_2)


    /**
     * Amazon Cognito for Artisans
     */
    private fun signInArtisan() {
        var email = email_et.text.toString()
        var password = password_et.text.toString()
        //TODO for testing remove before release
//        email = "jhuang81@calpoly.edu"
//        password = "HBp7X6gXdNa0"

        var user = userPool.getUser(email)


        var authenticateUser = object : AuthenticationHandler {
            override fun onSuccess(userSession: CognitoUserSession?, newDevice: CognitoDevice?) {
                // Sign-in was successful, cognitoUserSession will contain tokens for the user
                Log.d("LoginScreen", "in authHandlerLogin success")
                // go to home screen

                getArtisanAndCheckPassword(email)
            }

            override fun getAuthenticationDetails(authenticationContinuation: AuthenticationContinuation?, userId: String?) {
                Log.d("LoginScreen", "Getting authentication details from login")
                // The API needs user sign-in credentials to continue
                val authenticationDetails = AuthenticationDetails(userId, password, null)

                // Pass the user sign-in credentials to the continuation
                authenticationContinuation?.setAuthenticationDetails(authenticationDetails)

                // Allow the sign-in to continue
                authenticationContinuation?.continueTask()
            }

            override fun getMFACode(continuation: MultiFactorAuthenticationContinuation?) {
                // Multi-factor authentication is required, get the verification code from user
                //continuation?.setMfaCode(mfaVerificationCode)
                // Allow the sign-in process to continue
                continuation?.continueTask()
            }

            // Method is called when user logs in for first time with temp password
            override fun authenticationChallenge(continuation: ChallengeContinuation?) {
                Log.d("LoginScreen", "New user with temp password logging in")
                // Check the challenge name
                if("NEW_PASSWORD_REQUIRED".equals(continuation?.challengeName)) {
                    // A new user is trying to sign in for the first time after
                    // admin has created the user’s account
                    // Cast to NewPasswordContinuation for easier access to challenge parameters
                    var newPasswordContinuation : NewPasswordContinuation? = continuation as NewPasswordContinuation;

                    // Get the list of required parameters
                    var requiredAttributes = newPasswordContinuation?.requiredAttributes

                    // Get the current user attributes
                    var currUserAttributes = newPasswordContinuation?.currentUserAttributes

                    // Prompt user to set a new password and values for required attributes

                    // Set new user password
                    newPasswordContinuation?.setPassword(password)


                    // Allow the sign-in to complete
                    newPasswordContinuation?.continueTask();
                }
            }

            override fun onFailure(exception: Exception?) {
                Log.d("LoginScreen", "in authHandlerLogin fail")
                Log.d("LoginScreen", exception?.message)

                Toast.makeText(this@LoginScreen, "Unable to login. Please confirm your " +
                        "email and password are correct AND that you have confirmed your email.", Toast.LENGTH_LONG)
            }
        }


        user.getSessionInBackground(authenticateUser)
    }



    private fun getArtisanAndCheckPassword(email: String) {
        val requestBody = FormBody.Builder().add("email", email)
                .build()
        val client = OkHttpClient()
        val request = Request.Builder()
                .url(getArtisanUrl)
                .post(requestBody)
                .build()
        Log.d("LoginScreen", "In getARtsian with email: "+email)
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response?) {
                val body = response?.body()?.string()
                var artisans = listOf<Artisan>()
                Log.d("LoginScreen", "response body from fetchArtisan: " + body)

                val gson = GsonBuilder().create()

                if (body == "{}") {
                    Log.d("LoginScreen", "artisan not in db")
                }

                try { // In here, might need to set artisanNameTV.text = artisan.artisanName
                    artisans = gson.fromJson(body, object : TypeToken<List<Artisan>>() {}.type)

                    Log.d("LoginScreen", "Going to artisan home page")
                    val intent =  Intent(this@LoginScreen, HomeScreenArtisan::class.java)
                    intent.putExtra("artisan", artisans[0])
                    startActivity(intent)

                } catch(e: Exception) {
                    Log.d("LoginScreen", "Caught exception: "+e.message)

                }

            }

            override fun onFailure(call: Call?, e: IOException?) {
                Log.e("LoginScreen", "failed to do POST request to database" + getArtisanUrl)
            }
        })
    }

    private fun updateArtisanPassword(artisan: Artisan, email: String){
        val intent =  Intent(this@LoginScreen, ArtisanUpdatePassword::class.java)
        intent.putExtra("email", email)
        intent.putExtra("artisan", artisan)
        startActivity(intent)
        finish()
    }


    /**
     * Amazon OAuth for CGAs
     */
    private var signUpListener = object  : AuthorizeListener() {
        /* Authorization was completed successfully. */
        override fun onSuccess(result: AuthorizeResult) {
            /* Your app is now authorized for the requested scopes */
            Log.d("LoginScreen", "successful signup: "+result)
            val intent = Intent(this@LoginScreen, HomeScreen::class.java)
            startActivity(intent)
            finish()
        }

        /* There was an error during the attempt to authorize the application. */
        override fun onError(ae: AuthError) {
            /* Inform the user of the error */
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            alertDialog.dismiss()
        }

        /* Authorization was cancelled before it could be completed. */
        override fun onCancel(cancellation: AuthCancellation) {
            /* Reset the UI to a ready-to-login state */
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            alertDialog.dismiss()
        }
    }

    private var checkTokenListener = object  : Listener<AuthorizeResult, AuthError> {
        override fun onSuccess(ar: AuthorizeResult?) {
            if(ar?.accessToken != null) { //user already signed in to app
                val intent = Intent(this@LoginScreen, HomeScreen::class.java)
                startActivity(intent)
                Log.d("LoginScreen", ar?.accessToken)
                finish()
            }
            else {
                Log.d("LoginScreen", "token not found: "+ar)
            }
        }

        override fun onError(ae: AuthError?) {
            //To change body of created functions use File | Settings | File Templates.
            Log.d("LoginScreen", "error geting token: "+ae)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_screen)


        requestContext.registerListener(signUpListener)

        //TODO implement Artisan login
        artisan_log_in_button.setOnClickListener {
            signInArtisan()
        }

        cga_log_in_button.setOnClickListener{
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

            AuthorizationManager.authorize(AuthorizeRequest
                    .Builder(requestContext)
                    .addScopes(ProfileScope.profile(), ProfileScope.postalCode(), ProfileScope.profile())
                    .showProgress(true)// if you change these, need to also change the scopes val at top to match
                    .build())

            alertDialog = AlertDialog.Builder(this@LoginScreen).create()
            alertDialog.setTitle("Logging In")
            alertDialog.setMessage("Please wait while login is completed...")
            alertDialog.setCanceledOnTouchOutside(false)
            alertDialog.show()
        }

        log_in_layout.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, m: MotionEvent): Boolean {
                hideKeyboard(v)
                return true
            }
        })
    }

    override fun onStart() {
        super.onStart()
        AuthorizationManager.getToken(this, scopes, checkTokenListener)
    }

    override fun onResume() {
        super.onResume()
        requestContext.onResume()
    }

    private fun validateInput() : Boolean {
        if (email_et.text.toString().isEmpty()){
            email_til.error = this.resources.getString(R.string.requiredFieldError)
            return false
        }

        if (!(email_et.text.toString().contains("@"))){
            email_til.error = this.resources.getString(R.string.error_invalid_email)
            return false
        }

        if (password_et.text.toString().isEmpty()){
            password_til.error = this.resources.getString(R.string.requiredFieldError)
            return false
        }

        return true
    }

    private fun test() {
        // go to home screen
        val intent =  Intent(this, HomeScreenArtisan::class.java)
        startActivity(intent)
    }

    private fun hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

}
