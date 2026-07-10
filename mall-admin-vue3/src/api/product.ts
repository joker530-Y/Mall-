import request, { PageResult, unwrap } from './request'

export interface Product {
  id: number
  name: string
  pic?: string
  productSn?: string
  brandId?: number
  productCategoryId?: number
  price?: number
  stock?: number
  publishStatus?: number
  verifyStatus?: number
  newStatus?: number
  recommandStatus?: number
  sale?: number
  subTitle?: string
}

export interface ProductCategory {
  id: number
  parentId: number
  name: string
  level: number
  productCount?: number
  showStatus?: number
  navStatus?: number
  sort?: number
  children?: ProductCategory[]
}

export interface Brand {
  id: number
  name: string
  firstLetter?: string
  sort?: number
  factoryStatus?: number
  showStatus?: number
  logo?: string
  productCount?: number
}

export interface ProductAttribute {
  id: number
  productAttributeCategoryId?: number
  name: string
  selectType?: number
  inputType?: number
  inputList?: string
  sort?: number
  filterType?: number
  searchType?: number
  relatedStatus?: number
  handAddStatus?: number
  type?: number
}

export interface ProductQuery {
  keyword?: string
  productSn?: string
  publishStatus?: number
  verifyStatus?: number
  productCategoryId?: number
  brandId?: number
  pageNum: number
  pageSize: number
}

export function listProducts(params: ProductQuery) {
  return unwrap<PageResult<Product>>(request.get('/product/list', { params }))
}

export function getProductUpdateInfo(id: number) {
  return unwrap<Record<string, unknown>>(request.get(`/product/updateInfo/${id}`))
}

export function updateProduct(id: number, data: Record<string, unknown>) {
  return unwrap<unknown>(request.post(`/product/update/${id}`, data))
}

export function updateProductPublishStatus(ids: number[], publishStatus: number) {
  return unwrap<unknown>(request.post('/product/update/publishStatus', null, { params: { ids, publishStatus } }))
}

export function updateProductDeleteStatus(ids: number[], deleteStatus: number) {
  return unwrap<unknown>(request.post('/product/update/deleteStatus', null, { params: { ids, deleteStatus } }))
}

export function listCategoriesWithChildren() {
  return unwrap<ProductCategory[]>(request.get('/productCategory/list/withChildren'))
}

export function listCategories(parentId: number, pageNum: number, pageSize: number) {
  return unwrap<PageResult<ProductCategory>>(request.get(`/productCategory/list/${parentId}`, { params: { pageNum, pageSize } }))
}

export function deleteCategory(id: number) {
  return unwrap<unknown>(request.post(`/productCategory/delete/${id}`))
}

export function listBrands(params: { keyword?: string; showStatus?: number; pageNum: number; pageSize: number }) {
  return unwrap<PageResult<Brand>>(request.get('/brand/list', { params }))
}

export function listAllBrands() {
  return unwrap<Brand[]>(request.get('/brand/listAll'))
}

export function updateBrandShowStatus(ids: number[], showStatus: number) {
  return unwrap<unknown>(request.post('/brand/update/showStatus', null, { params: { ids, showStatus } }))
}

export function listProductAttributes(cid: number, type: number, pageNum: number, pageSize: number) {
  return unwrap<PageResult<ProductAttribute>>(request.get(`/productAttribute/list/${cid}`, { params: { type, pageNum, pageSize } }))
}

export function deleteProductAttributes(ids: number[]) {
  return unwrap<unknown>(request.post('/productAttribute/delete', null, { params: { ids } }))
}
