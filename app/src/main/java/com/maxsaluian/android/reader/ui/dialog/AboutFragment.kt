package com.maxsaluian.android.reader.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.maxsaluian.android.reader.R
import com.maxsaluian.android.reader.util.extensions.addRipple

class AboutFragment : BottomSheetDialogFragment() {

    interface Callback {
        fun onGoToRepoClicked()
    }

    private lateinit var titleTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var imageView: ImageView
    private lateinit var goButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_about, container, false)
        titleTextView = view.findViewById(R.id.textView_title)
        descriptionTextView = view.findViewById(R.id.textView_description)
        imageView = view.findViewById(R.id.imageView_feed)
        goButton = view.findViewById(R.id.button_positive)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        descriptionTextView.addRipple()
        imageView.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources,
                R.mipmap.ic_launcher_round,
                null
            )
        )

        goButton.apply {
            text = getString(R.string.go)
            setOnClickListener {
                targetFragment?.let { (it as Callback).onGoToRepoClicked() }
                dismiss()
            }
        }
    }

    companion object {

        fun newInstance(): AboutFragment {
            return AboutFragment()
        }
    }
}