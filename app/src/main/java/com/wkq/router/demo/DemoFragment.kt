package com.wkq.router.demo

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.wkq.router.annotation.Route

@Route(path = "/demo/fragment")
class DemoFragment : Fragment() {
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return TextView(requireContext()).apply {
            text = arguments?.getString("title") ?: "DemoFragment"
            textSize = 18f
            setPadding(24, 24, 24, 24)
        }
    }
}
