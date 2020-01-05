package com.ryanjames.swabergersmobilepos.mappers

import com.ryanjames.swabergersmobilepos.database.realm.*
import com.ryanjames.swabergersmobilepos.domain.*
import com.ryanjames.swabergersmobilepos.network.responses.*
import io.realm.RealmList

class MenuMapper : DataMapper<MenuRealmEntity, MenuResponse, Menu> {

    private val categoryMapper = CategoryMapper()
    private val modifierGroupMapper = ModifierGroupMapper()
    private val productMapper = ProductMapper()
    private val productBundleMapper = ProductBundleMapper()
    private val productGroupMapper = ProductGroupMapper()

    override fun mapToEntity(input: MenuResponse): MenuRealmEntity {
        val categoryRealmList = RealmList<CategoryRealmEntity>()
        val modifierGroupRealmList = RealmList<ModifierGroupRealmEntity>()
        val productRealmList = RealmList<ProductRealmEntity>()
        val bundleRealmList = RealmList<ProductBundleRealmEntity>()
        val productGroupRealmList = RealmList<ProductGroupRealmEntity>()

        input.modifierGroups?.let {
            val modifierGroupList = modifierGroupMapper.mapToEntity(input.modifierGroups)
            modifierGroupRealmList.addAll(modifierGroupList)
        }

        input.bundles?.forEach { bundleResponse ->
            val bundleEntity = productBundleMapper.mapToEntity(bundleResponse)
            bundleRealmList.add(bundleEntity)
        }

        input.products?.forEach { productResponse ->
            val productEntity = productMapper.mapToEntity(productResponse)

            productResponse.modifierGroups?.forEach { modifierGroupId ->
                modifierGroupRealmList.find { it.modifierGroupId == modifierGroupId }?.let { productEntity.modifierGroups.add(it) }
            }

            productResponse.bundles?.forEach { bundleId ->
                bundleRealmList.find { it.bundleId == bundleId }?.let { productEntity.bundles.add(it) }
            }

            productRealmList.add(productEntity)
        }

        input.productGroups?.forEach { productGroupResponse ->
            val productGroupEntity = productGroupMapper.mapToEntity(productGroupResponse)

            productGroupResponse.defaultProduct?.let { productId ->
                productRealmList.find { it.productId == productId }?.let { productGroupEntity.defaultProduct = it }
            }

            productGroupResponse.options?.forEach { productId ->
                productRealmList.find { it.productId == productId }?.let { productGroupEntity.options.add(it) }
            }

            productGroupRealmList.add(productGroupEntity)
        }

        // Adding product groups to bundles
        input.bundles?.forEach { bundleResponse ->
            val bundleRealm = bundleRealmList.find { it.bundleId == bundleResponse.bundleId }
            bundleRealm?.let {
                bundleResponse.productGroups?.forEach { productGroupId ->
                    productGroupRealmList.find { it.productGroupId == productGroupId }?.let { bundleRealm.productGroups.add(it) }
                }
            }
        }

        input.categories?.forEach { categoryResponse ->
            val categoryEntity = categoryMapper.mapToEntity(categoryResponse)
            categoryResponse.products?.forEach { productId ->
                productRealmList.find { it.productId == productId }?.let { categoryEntity.products.add(it) }
            }
            categoryRealmList.add(categoryEntity)
        }

        return MenuRealmEntity(categoryRealmList)
    }

    override fun mapToDomain(input: MenuRealmEntity): Menu {
        return Menu(categoryMapper.mapToDomain(input.categories))
    }

}

private class CategoryMapper : DataMapper<CategoryRealmEntity, CategoryResponse, Category> {

    private val productMapper = ProductMapper()

    override fun mapToEntity(input: CategoryResponse): CategoryRealmEntity {
        if (input.categoryId != null && input.categoryName != null) {
            return CategoryRealmEntity(input.categoryId, input.categoryName, RealmList())
        }
        return CategoryRealmEntity()
    }

    override fun mapToDomain(input: CategoryRealmEntity): Category {
        return Category(input.categoryId, input.categoryName, productMapper.mapToDomain(input.products))
    }

}


private class ProductMapper : DataMapper<ProductRealmEntity, ProductResponse, Product> {

    private val modifierGroupMapper = ModifierGroupMapper()
    private val bundleMapper = ProductBundleMapper()

    override fun mapToEntity(input: ProductResponse): ProductRealmEntity {
        if (input.productId == null || input.productName == null) return ProductRealmEntity()
        return ProductRealmEntity(
            input.productId,
            input.productName,
            input.price ?: 0f,
            input.receiptText ?: "",
            RealmList(),
            RealmList()
        )
    }

    override fun mapToDomain(input: ProductRealmEntity): Product {
        return Product(
            input.productId,
            input.productName,
            input.price,
            input.receiptText,
            bundleMapper.mapToDomain(input.bundles),
            modifierGroupMapper.mapToDomain(input.modifierGroups)
        )
    }
}

private class ProductBundleMapper : DataMapper<ProductBundleRealmEntity, BundleResponse, ProductBundle> {

    private val productGroupMapper = ProductGroupMapper()

    override fun mapToEntity(input: BundleResponse): ProductBundleRealmEntity {
        if (input.bundleId == null || input.bundleName == null) return ProductBundleRealmEntity()
        return ProductBundleRealmEntity(
            input.bundleId,
            input.bundleName,
            input.price ?: 0f,
            input.receiptText ?: "",
            RealmList()
        )
    }


    override fun mapToDomain(input: ProductBundleRealmEntity): ProductBundle {
        return ProductBundle(
            input.bundleId,
            input.bundleName,
            input.price,
            input.receiptText,
            productGroupMapper.mapToDomain(input.productGroups)
        )
    }

}

private class ProductGroupMapper : DataMapper<ProductGroupRealmEntity, ProductGroupResponse, ProductGroup> {

    private val modifierGroupMapper = ModifierGroupMapper()

    override fun mapToEntity(input: ProductGroupResponse): ProductGroupRealmEntity {
        if (input.productGroupId == null || input.productGroupName == null) return ProductGroupRealmEntity()
        return ProductGroupRealmEntity(
            input.productGroupId,
            input.productGroupName,
            RealmList()
        )
    }

    override fun mapToDomain(input: ProductGroupRealmEntity): ProductGroup {
        return ProductGroup(
            input.productGroupId,
            input.productGroupName,
            mapToEmptyBundleDomain(input.defaultProduct ?: ProductRealmEntity()),
            mapToEmptyBundleDomain(input.options)
        )
    }


    private fun mapToEmptyBundleDomain(input: ProductRealmEntity): Product {
        return Product(
            input.productId,
            input.productName,
            input.price,
            input.receiptText,
            listOf(),
            modifierGroupMapper.mapToDomain(input.modifierGroups)
        )
    }

    private fun mapToEmptyBundleDomain(input: List<ProductRealmEntity>): List<Product> {
        return input.map { mapToEmptyBundleDomain(it) }
    }

}

private class ModifierInfoMapper : DataMapper<ModifierInfoRealmEntity, ModifierInfoResponse, ModifierInfo> {

    override fun mapToEntity(input: ModifierInfoResponse): ModifierInfoRealmEntity {
        if (input.modifierId == null || input.modifierName == null) return ModifierInfoRealmEntity()
        return ModifierInfoRealmEntity(input.modifierId, input.modifierName, input.priceDelta ?: 0f, input.receiptText ?: "")
    }

    override fun mapToDomain(input: ModifierInfoRealmEntity): ModifierInfo {
        return ModifierInfo(input.modifierId, input.modifierName, input.priceDelta, input.receiptText)
    }
}

private class ModifierGroupMapper : DataMapper<ModifierGroupRealmEntity, ModifierGroupResponse, ModifierGroup> {

    private val modifierInfoMapper = ModifierInfoMapper()

    override fun mapToEntity(input: ModifierGroupResponse): ModifierGroupRealmEntity {
        if (input.modifierGroupId == null || input.modifierGroupName == null) return ModifierGroupRealmEntity()

        val modifierInfoRealmList = RealmList<ModifierInfoRealmEntity>()
        input.options?.let {
            modifierInfoRealmList.addAll(modifierInfoMapper.mapToEntity(it))
        }

        return ModifierGroupRealmEntity(
            input.modifierGroupId,
            input.modifierGroupName,
            input.action ?: "",
            modifierInfoRealmList,
            input.defaultSelection ?: ""
        )
    }

    override fun mapToDomain(input: ModifierGroupRealmEntity): ModifierGroup {
        return ModifierGroup(
            input.modifierGroupId,
            input.modifierGroupName,
            input.action.toModifierGroupAction(),
            modifierInfoMapper.mapToDomain(input.getDefaultSelection() ?: ModifierInfoRealmEntity()),
            modifierInfoMapper.mapToDomain(input.options)
        )
    }

}


private fun String?.toModifierGroupAction(): ModifierGroupAction {
    return when (this) {
        "on" -> ModifierGroupAction.Required
        "add" -> ModifierGroupAction.Optional
        else -> ModifierGroupAction.Required
    }
}



