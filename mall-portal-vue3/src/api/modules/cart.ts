import request, { unwrap } from '../http/request'

export interface CartItem {
  id: number
  productId: number
  productSkuId: number
  quantity: number
  price?: number
  productName?: string
  productPic?: string
  productAttr?: string
}

export function listCartItems() {
  return unwrap<CartItem[]>(request.get('/cart/list'))
}

export function addCartItem(item: Partial<CartItem>) {
  return unwrap<number>(request.post('/cart/add', item))
}

export function updateCartQuantity(id: number, quantity: number) {
  return unwrap<number>(request.get('/cart/update/quantity', { params: { id, quantity } }))
}

export function deleteCartItems(ids: number[]) {
  return unwrap<number>(request.post('/cart/delete', null, { params: { ids } }))
}

export function clearCart() {
  return unwrap<number>(request.post('/cart/clear'))
}
