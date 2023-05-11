package com.frkn.notesapp.fragments

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.frkn.notesapp.R
import com.frkn.notesapp.activities.MainActivity
import com.frkn.notesapp.adapters.RecyclerNoteAdapter
import com.frkn.notesapp.databinding.FragmentNoteBinding
import com.frkn.notesapp.utils.SwipetoDelete
import com.frkn.notesapp.utils.hideKeybord
import com.frkn.notesapp.viewModel.NoteActivityViewModel
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialElevationScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class NoteFragment : Fragment(R.layout.fragment_note) {

    private lateinit var noteBinding: FragmentNoteBinding
    private val noteActivityViewModel : NoteActivityViewModel by  activityViewModels()
    private lateinit var  rvAdapter : RecyclerNoteAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = MaterialElevationScale(false).apply {
            duration=350
        }

        enterTransition = MaterialElevationScale(true).apply {
            duration = 350
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        noteBinding = FragmentNoteBinding.bind(view)
        val activity = activity as MainActivity
        val navController = Navigation.findNavController(view)
        requireView().hideKeybord()
        CoroutineScope(Dispatchers.Main).launch {
            delay(10)
            //activity.window.statusBarColor = Color.WHITE
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            activity.window.statusBarColor = Color.parseColor("#9E9D9D")

        }

        noteBinding.addNoteFab.setOnClickListener {
            noteBinding.appBarLayout.visibility=View.INVISIBLE
            navController.navigate(NoteFragmentDirections.actionNoteFragmentToSaveOrUpdateFragment())
        }

        noteBinding.innerFab.setOnClickListener {
            noteBinding.appBarLayout.visibility=View.INVISIBLE
            navController.navigate(NoteFragmentDirections.actionNoteFragmentToSaveOrUpdateFragment())
        }


        recyclerViewDisplay()
        swipeToDelete(noteBinding.recyclerNote)

        noteBinding.search.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                noteBinding.noData.isVisible = false
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if(p0.toString().isNotEmpty()){
                    val text = p0.toString()
                    val query = "%$text"
                    if (query.isNotEmpty()){
                        noteActivityViewModel.searchNote(query).observe(viewLifecycleOwner){
                            rvAdapter.submitList(it)
                        }
                    }
                    else{
                        observerDataChanges()
                    }
                }
                else{
                    observerDataChanges()
                }
            }

            override fun afterTextChanged(p0: Editable?) {

            }

        })

        noteBinding.search.setOnEditorActionListener { v, actionId, _ ->
            if(actionId==EditorInfo.IME_ACTION_SEARCH){
                v.clearFocus()
                requireView().hideKeybord()
            }
            return@setOnEditorActionListener true
        }

        noteBinding.recyclerNote.setOnScrollChangeListener { _, scrollX, scrollY, _, oldScrollY ->
            when{
                scrollY>oldScrollY -> noteBinding.chatFabText.isVisible = false
                scrollX == scrollY -> noteBinding.chatFabText.isVisible = true
                else -> noteBinding.chatFabText.isVisible = true
            }
        }

    }

    private fun swipeToDelete(recyclerNote: RecyclerView) {

        val swipeToDeleteCallback = object : SwipetoDelete() {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                val note = rvAdapter.currentList[position]
                var actionBtnTapped = false
                noteActivityViewModel.deleteNote(note)
                noteBinding.search.apply {
                    hideKeybord()
                    clearFocus()
                }
                if (noteBinding.search.text.toString().isEmpty()){
                    observerDataChanges()
                }

                val snackBar = Snackbar.make(requireView(),"Note Deleted",Snackbar.LENGTH_LONG)
                    .addCallback(object  : BaseTransientBottomBar.BaseCallback<Snackbar>(){
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            super.onDismissed(transientBottomBar, event)
                        }

                        override fun onShown(transientBottomBar: Snackbar?) {
                            transientBottomBar?.setAction("UNDO"){
                                noteActivityViewModel.saveNote(note)
                                actionBtnTapped = true
                                noteBinding.noData.isVisible = false
                            }
                            super.onShown(transientBottomBar)
                        }
                    }).apply {
                        animationMode = Snackbar.ANIMATION_MODE_FADE
                        setAnchorView(R.id.addNoteFab)
                    }
                    snackBar.setActionTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.yellowOrange
                        )
                    )
                    snackBar.show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(recyclerNote)
    }

    private fun observerDataChanges() {
        noteActivityViewModel.getAllNotes().observe(viewLifecycleOwner){ list ->
         noteBinding.noData.isVisible = list.isEmpty()
            rvAdapter.submitList(list)
        }
    }

    private fun recyclerViewDisplay() {

        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> setUpRecyclerView(2)
            Configuration.ORIENTATION_LANDSCAPE -> setUpRecyclerView(3)
        }
    }

    private fun setUpRecyclerView(spanCount: Int) {

        noteBinding.recyclerNote.apply {
            layoutManager = StaggeredGridLayoutManager(spanCount,StaggeredGridLayoutManager.VERTICAL)
            setHasFixedSize(true)
            rvAdapter = RecyclerNoteAdapter()
            rvAdapter.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            adapter = rvAdapter
            postponeEnterTransition(300L,TimeUnit.MILLISECONDS)
            viewTreeObserver.addOnPreDrawListener {
                startPostponedEnterTransition()
                true
            }
        }
        observerDataChanges()
    }
}