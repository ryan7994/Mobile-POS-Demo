package com.ryanjames.swabergersmobilepos.feature.bagsummary

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.ryanjames.swabergersmobilepos.R
import com.ryanjames.swabergersmobilepos.core.BaseActivity
import com.ryanjames.swabergersmobilepos.core.SwabergersApplication
import com.ryanjames.swabergersmobilepos.core.ViewModelFactory
import com.ryanjames.swabergersmobilepos.databinding.ActivityBagSummaryBinding
import com.ryanjames.swabergersmobilepos.domain.LineItem
import com.ryanjames.swabergersmobilepos.domain.Order
import com.ryanjames.swabergersmobilepos.feature.menuitemdetail.MenuItemDetailActivity
import com.ryanjames.swabergersmobilepos.feature.menuitemdetail.REQUEST_LINE_ITEM
import javax.inject.Inject

private const val EXTRA_ORDER = "extra.order"

class BagSummaryActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private lateinit var binding: ActivityBagSummaryBinding
    private lateinit var viewModel: BagSummaryViewModel
    private lateinit var order: Order
    private lateinit var adapter: BagItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SwabergersApplication.appComponent.inject(this)

        order = intent.getParcelableExtra(EXTRA_ORDER)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_bag_summary)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(BagSummaryViewModel::class.java)
        viewModel.order = order
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setToolbarTitle(getString(R.string.bag_summary_toolbar_title))
        setupRecyclerView()
        subscribe()
    }

    private fun subscribe() {
        viewModel.onOrderSucceeded.observe(this, Observer {
            AlertDialog.Builder(this)
                .setMessage(getString(R.string.order_created_message))
                .setPositiveButton(R.string.ok_cta) { dialogInterface, _ ->
                    viewModel.clearBag()
                    dialogInterface.dismiss()
                }.setCancelable(false)
                .show()
        })

        viewModel.orderFailed.observe(this, Observer {
            AlertDialog.Builder(this)
                .setMessage(getString(R.string.something_went_wrong))
                .setPositiveButton(getString(R.string.try_again_cta)) { dialogInterface, _ ->
                    dialogInterface.dismiss()
                    viewModel.postOrder()
                }
                .setNegativeButton(getString(R.string.later_cta)) { dialogInterface, _ -> dialogInterface.dismiss() }
                .show()
        })
    }


    private fun setupRecyclerView() {
        adapter = BagItemAdapter(order.lineItems, object : BagItemAdapter.BagItemAdapterListener {
            override fun onClickLineItem(lineItem: LineItem) {
                startActivityForResult(MenuItemDetailActivity.createIntent(this@BagSummaryActivity, lineItem), REQUEST_LINE_ITEM)
            }

        })
        binding.rvItems.also {
            it.layoutManager = LinearLayoutManager(this@BagSummaryActivity)
            it.adapter = this.adapter
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_LINE_ITEM && resultCode == Activity.RESULT_OK) {
            data?.let {
                val lineItem = MenuItemDetailActivity.getExtraLineItem(data)
                viewModel.putLineItem(lineItem)
                adapter.updateLineItems(viewModel.order.lineItems)
            }
        }
    }


    fun onClickCheckout(view: View) {
        // KeypadDialogFragment.show(supportFragmentManager)
        viewModel.postOrder()

    }

    companion object {

        fun createIntent(context: Context, order: Order): Intent {
            val intent = Intent(context, BagSummaryActivity::class.java)
            intent.putExtra(EXTRA_ORDER, order)
            return intent
        }

    }
}
