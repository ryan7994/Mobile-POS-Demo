package com.ryanjames.swabergersmobilepos.feature.bagsummary

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.ryanjames.swabergersmobilepos.R
import com.ryanjames.swabergersmobilepos.base.BaseActivity
import com.ryanjames.swabergersmobilepos.databinding.ActivityBagSummaryBinding
import com.ryanjames.swabergersmobilepos.domain.LineItem
import com.ryanjames.swabergersmobilepos.domain.OrderDetails
import com.ryanjames.swabergersmobilepos.feature.menuitemdetail.MenuItemDetailActivity
import com.ryanjames.swabergersmobilepos.feature.menuitemdetail.REQUEST_LINE_ITEM

private const val EXTRA_ORDER = "extra.order"

class BagSummaryActivity : BaseActivity() {

    private lateinit var binding: ActivityBagSummaryBinding
    private lateinit var viewModel: BagSummaryViewModel
    private lateinit var orderDetails: OrderDetails
    private lateinit var adapter: BagItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderDetails = intent.getParcelableExtra(EXTRA_ORDER)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_bag_summary)
        viewModel = ViewModelProviders.of(this, viewModelFactory { BagSummaryViewModel(orderDetails) }).get(BagSummaryViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        adapter = BagItemAdapter(orderDetails.lineItems, object : BagItemAdapter.BagItemAdapterListener {
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
                adapter.updateLineItems(viewModel.orderDetails.lineItems)
            }
        }
    }

    companion object {

        fun createIntent(context: Context, orderDetails: OrderDetails): Intent {
            val intent = Intent(context, BagSummaryActivity::class.java)
            intent.putExtra(EXTRA_ORDER, orderDetails)
            return intent
        }

    }
}