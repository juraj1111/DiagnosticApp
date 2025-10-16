package com.example.diagnosticapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.example.diagnosticapp.R

class DeleteProtocolFragment : DialogFragment() {

    interface DeleteProtocolListener {
        fun onProtocolDeleteConfirmed(protocolName: String)
    }

    private var protocolName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        protocolName = arguments?.getString("protocol_name")
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_delete_protocol, container, false)

        val btnDelete = view.findViewById<Button>(R.id.btnDelete)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        btnDelete.setOnClickListener {
            (activity as? DeleteProtocolListener)?.onProtocolDeleteConfirmed(protocolName ?: "")
            dismiss()
        }

        btnCancel.setOnClickListener { dismiss() }

        return view
    }

    companion object {
        fun newInstance(protocolName: String): DeleteProtocolFragment {
            val fragment = DeleteProtocolFragment()
            val args = Bundle()
            args.putString("protocol_name", protocolName)
            fragment.arguments = args
            return fragment
        }
    }
}
