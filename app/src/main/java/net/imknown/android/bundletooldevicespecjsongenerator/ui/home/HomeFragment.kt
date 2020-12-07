package net.imknown.android.bundletooldevicespecjsongenerator.ui.home

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import net.imknown.android.bundletooldevicespecjsongenerator.EventObserver
import net.imknown.android.bundletooldevicespecjsongenerator.R

class HomeFragment : Fragment() {

    private val homeViewModel by activityViewModels<HomeViewModel>()

    private lateinit var textView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        textView = root.findViewById(R.id.text_home)
        textView.movementMethod = ScrollingMovementMethod()

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        homeViewModel.addGlSurfaceViewEvent.observe(viewLifecycleOwner, EventObserver {
            (view as ViewGroup).addView(it)
        })

        homeViewModel.removeGlSurfaceViewEvent.observe(viewLifecycleOwner, EventObserver {
            (view as ViewGroup).removeView(it)
            it.holder.surface.release()
        })

        homeViewModel.result.observe(viewLifecycleOwner) {
            textView.text = it
        }

        homeViewModel.fetch()
    }
}