import request, { PageResult, unwrap } from '../http/request'

export interface Product {
  id: number
  name: string
  pic?: string
  price?: number
  promotionPrice?: number
  sale?: number
  subTitle?: string
  productCategoryId?: number
  brandId?: number
}

export interface ProductCategoryNode {
  id: number
  name: string
  children?: ProductCategoryNode[]
}

export interface SkuStock {
  id: number
  productId: number
  skuCode?: string
  price?: number
  stock?: number
  pic?: string
  /** JSON: [{"key":"颜色","value":"黑色"}, ...] */
  spData?: string
  /** 旧字段，部分历史数据可能仍有 */
  sp1?: string
  sp2?: string
  sp3?: string
}

export interface ProductAttribute {
  id: number
  name: string
  /** 0=规格，1=参数 */
  type?: number
}

export interface ProductAttributeValue {
  id: number
  productAttributeId: number
  value: string
}

export interface ProductDetail {
  product: Product
  brand?: { id: number; name: string }
  skuStockList: SkuStock[]
  productAttributeList: ProductAttribute[]
  productAttributeValueList: ProductAttributeValue[]
}

export function searchProducts(params: {
  keyword?: string
  brandId?: number
  productCategoryId?: number
  pageNum: number
  pageSize: number
  sort?: number
}) {
  return unwrap<PageResult<Product>>(request.get('/product/search', { params }))
}

export function getProductDetail(id: number) {
  return unwrap<ProductDetail>(request.get(`/product/detail/${id}`))
}

export function getCategoryTree() {
  return unwrap<ProductCategoryNode[]>(request.get('/product/categoryTreeList'))
}
