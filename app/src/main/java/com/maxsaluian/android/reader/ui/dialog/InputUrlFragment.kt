package com.maxsaluian.android.reader.ui.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.maxsaluian.android.reader.R
import com.maxsaluian.android.reader.ui.FeedRequestCallbacks
import com.maxsaluian.android.reader.util.extensions.isVisible
import com.maxsaluian.android.reader.util.extensions.show
import com.maxsaluian.android.reader.util.extensions.toEditable

class InputUrlFragment : BottomSheetDialogFragment() {

    private lateinit var urlEditText: EditText
    private lateinit var subscribeButton: Button
    private lateinit var progressBar: ProgressBar

    private var callbacks: FeedRequestCallbacks? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogNoFloating)
        callbacks = targetFragment as? FeedRequestCallbacks
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        urlEditText.apply {
            text = arguments?.getString(ARG_LAST_URL).toEditable()
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    subscribeButton.isEnabled = s?.isNotEmpty() == true
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) submitFeedUrl(urlEditText.text.toString())
                true
            }
        }

        subscribeButton.apply {
            isEnabled = urlEditText.text.isNotEmpty()
            setOnClickListener {
                submitFeedUrl(urlEditText.text.toString())
                it.isEnabled = false
            }
        }
    }

    private fun submitFeedUrl(url: String) {
        callbacks?.onRequestSubmitted(url)
        progressBar.show()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        if (progressBar.isVisible()) callbacks?.onRequestDismissed()
    }

    companion object {

        const val TAG = "InputUrlFragment"
        private const val ARG_LAST_URL = "ARG_LAST_URL"

        fun newInstance(lastAttemptedUrl: String): InputUrlFragment {
            val args = Bundle().apply { putString(ARG_LAST_URL, lastAttemptedUrl) }
            return InputUrlFragment().apply { arguments = args }
        }
    }
}