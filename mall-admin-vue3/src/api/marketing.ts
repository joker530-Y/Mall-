import request, { PageResult, unwrap } from './request'

export interface Coupon {
  id: number
  name: string
  type?: number
  amount?: number
  minPoint?: number
  perLimit?: number
  publishCount?: number
  useCount?: number
  receiveCount?: number
  startTime?: string
  endTime?: string
  note?: string
}

export interface HomeAdvertise {
  id: number
  name: string
  type?: number
  pic?: string
  startTime?: string
  endTime?: string
  status?: number
  url?: string
  note?: string
  sort?: number
}

export interface HomeRecommendItem {
  id: number
  productId?: number
  productName?: string
  brandId?: number
  brandName?: string
  subjectId?: number
  subjectName?: string
  recommendStatus?: number
  sort?: number
}

export function listCoupons(params: { name?: string; type?: number; pageNum: number; pageSize: number }) {
  return unwrap<PageResult<Coupon>>(request.get('/coupon/list', { params }))
}

export function deleteCoupon(id: number) {
  return unwrap<unknown>(request.post(`/coupon/delete/${id}`))
}

export function listHomeAdvertises(params: { name?: string; type?: number; pageNum: number; pageSize: number }) {
  return unwrap<PageResult<HomeAdvertise>>(request.get('/home/advertise/list', { params }))
}

export function updateHomeAdvertiseStatus(id: number, status: number) {
  return unwrap<unknown>(request.post(`/home/advertise/update/status/${id}`, null, { params: { status } }))
}

export function listHomeNewProducts(params: { productName?: string; recommendStatus?: number; pageNum: number; pageSize: number }) {
  return unwrap<PageResult<HomeRecommendItem>>(request.get('/home/newProduct/list', { params }))
}

export function updateHomeNewProductRecommendStatus(ids: number[], recommendStatus: number) {
  return unwrap<unknown>(request.post('/home/newProduct/update/recommendStatus', null, { params: { ids, recommendStatus } }))
}

export function listHomeBrands(params: { brandName?: string; recommendStatus?: number; pageNum: number; pageSize: number }) {
  return unwrap<PageResult<HomeRecommendItem>>(request.get('/home/brand/list', { params }))
}

export function updateHomeBrandRecommendStatus(ids: number[], recommendStatus: number) {
  return unwrap<unknown>(request.post('/home/brand/update/recommendStatus', null, { params: { ids, recommendStatus } }))
}

export function listHomeRecommendProducts(params: { productName?: string; recommendStatus?: number; pageNum: number; pageSize: number }) {
  return unwrap<PageResult<HomeRecommendItem>>(request.get('/home/recommendProduct/list', { params }))
}

export function updateHomeRecommendProductStatus(ids: number[], recommendStatus: number) {
  return unwrap<unknown>(request.post('/home/recommendProduct/update/recommendStatus', null, { params: { ids, recommendStatus } }))
}

export function listHomeRecommendSubjects(params: { subjectName?: string; recommendStatus?: number; pageNum: number; pageSize: number }) {
  return unwrap<PageResult<HomeRecommendItem>>(request.get('/home/recommendSubject/list', { params }))
}

export function updateHomeRecommendSubjectStatus(ids: number[], recommendStatus: number) {
  return unwrap<unknown>(request.post('/home/recommendSubject/update/recommendStatus', null, { params: { ids, recommendStatus } }))
}
