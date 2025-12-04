package com.example.myapplication

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.myapplication.db.AppDatabase
import com.example.myapplication.network.socket.ChatSocketClient
import com.example.myapplication.repository.ChatRepository
import com.example.myapplication.ui.screen.ChatScreenContainer
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    
    private lateinit var database: AppDatabase
    private lateinit var repository: ChatRepository
    private lateinit var socketClient: ChatSocketClient
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions result
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize database and socket
        database = AppDatabase.getInstance(this)
        repository = ChatRepository(database)
        socketClient = ChatSocketClient("192.168.1.100", 8888) // Configure server address
        
        // Request necessary permissions
        requestPermissions()
        
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    ChatScreenContainer(
                        repository = repository,
                        socketClient = socketClient
                    )
                }
            }
        }
    }
    
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up socket connection
        Thread {
            runBlocking {
                socketClient.disconnect()
            }
        }.start()
    }
}