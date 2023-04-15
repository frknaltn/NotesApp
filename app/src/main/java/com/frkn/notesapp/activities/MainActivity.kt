package com.frkn.notesapp.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.frkn.notesapp.R
import com.frkn.notesapp.database.NoteDatabase
import com.frkn.notesapp.databinding.ActivityMainBinding
import com.frkn.notesapp.repository.NoteRepository
import com.frkn.notesapp.viewModel.NoteActivityViewModel
import com.frkn.notesapp.viewModel.NoteActivityViewModelFactory

class MainActivity : AppCompatActivity() {

    lateinit var noteActivityViewModel : NoteActivityViewModel
    private lateinit var binding : ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityMainBinding.inflate(layoutInflater)

        try {
            setContentView(binding.root)
            val noteRepository = NoteRepository(NoteDatabase(this))
            val noteActivityViewModelFactory = NoteActivityViewModelFactory(noteRepository)
            noteActivityViewModel = ViewModelProvider(this,
            noteActivityViewModelFactory)[NoteActivityViewModel::class.java]
        }catch (e: Exception){
            Log.d("TAG","Error")
        }
    }

}