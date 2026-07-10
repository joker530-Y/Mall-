import request, { unwrap } from '../http/request'

export interface MemberAddress {
  id: number
  name: string
  phoneNumber: string
  defaultStatus?: number
  province?: string
  city?: string
  region?: string
  detailAddress?: string
}

export function listAddresses() {
  return unwrap<MemberAddress[]>(request.get('/member/address/list'))
}

export function addAddress(address: Partial<MemberAddress>) {
  return unwrap<number>(request.post('/member/address/add', address))
}

export function updateAddress(id: number, address: Partial<MemberAddress>) {
  return unwrap<number>(request.post(`/member/address/update/${id}`, address))
}

export function deleteAddress(id: number) {
  return unwrap<number>(request.post(`/member/address/delete/${id}`))
}
