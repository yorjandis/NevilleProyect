package com.ypg.neville.ui.frag

import androidx.activity.OnBackPressedCallback
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.commitNow
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.ypg.neville.R

class SheetNavHostBottomSheet : DialogFragment() {

    private var containerId: Int = View.generateViewId()
    private var childNavController: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_NevilleProyect_CenterDialog)
    }

    override fun onStart() {
        super.onStart()
        val bgColor = resolveThemeBackgroundColor()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setGravity(Gravity.CENTER)
            setBackgroundDrawable(bgColor.toDrawable())
            setWindowAnimations(R.style.NevilleBottomDialogAnimation)
        }
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return androidx.fragment.app.FragmentContainerView(requireContext()).apply {
            id = containerId
            setBackgroundColor(resolveThemeBackgroundColor())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.alpha = 0f
        view.scaleX = 0.92f
        view.scaleY = 0.92f
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(220L)
            .start()

        if (childFragmentManager.findFragmentById(containerId) != null) return

        val navHost = NavHostFragment()
        childFragmentManager.commitNow {
            replace(containerId, navHost)
        }

        val startDestination = requireArguments().getInt(ARG_START_DESTINATION)
        val navController = navHost.navController
        childNavController = navController
        val graph = navController.navInflater.inflate(R.navigation.nav_graf)
        graph.setStartDestination(startDestination)
        val startArgs = requireArguments().getBundle(ARG_START_ARGS) ?: Bundle()
        navController.setGraph(graph, startArgs)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val controller = childNavController
                    if (controller != null && controller.currentDestination?.id != controller.graph.startDestinationId) {
                        controller.popBackStack()
                    } else {
                        dismiss()
                    }
                }
            }
        )
    }

    companion object {
        private const val ARG_START_DESTINATION = "arg_start_destination"
        private const val ARG_START_ARGS = "arg_start_args"

        fun newInstance(startDestination: Int, startArgs: Bundle? = null): SheetNavHostBottomSheet {
            return SheetNavHostBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_START_DESTINATION, startDestination)
                    startArgs?.let { putBundle(ARG_START_ARGS, it) }
                }
            }
        }
    }

    private fun resolveThemeBackgroundColor(): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        return typedValue.data
    }
}
