import request, { unwrap } from '../http/request'
import type { Product } from './product'

export interface HomeAdvertise {
  id: number
  name: string
  pic?: string
  url?: string
}

export interface FlashPromotionProduct extends Product {
  relationId?: number
  flashPromotionPrice?: number
  flashPromotionCount?: number
  flashPromotionLimit?: number
}

export interface HomeFlashPromotion {
  startTime?: string
  endTime?: string
  nextStartTime?: string
  nextEndTime?: string
  productList?: FlashPromotionProduct[]
}

export interface HomeContent {
  advertiseList?: HomeAdvertise[]
  brandList?: { id: number; name: string; logo?: string }[]
  homeFlashPromotion?: HomeFlashPromotion
  newProductList?: Product[]
  hotProductList?: Product[]
  subjectList?: { id: number; title: string; pic?: string }[]
}

export function getHomeContent() {
  return unwrap<HomeContent>(request.get('/home/content'))
}

export function getHotProducts(pageNum = 1, pageSize = 6) {
  return unwrap<Product[]>(request.get('/home/hotProductList', { params: { pageNum, pageSize } }))
}

export function getNewProducts(pageNum = 1, pageSize = 6) {
  return unwrap<Product[]>(request.get('/home/newProductList', { params: { pageNum, pageSize } }))
}
