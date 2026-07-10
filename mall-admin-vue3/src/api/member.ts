import request, { unwrap } from './request'

export interface MemberLevel {
  id: number
  name: string
  growthPoint?: number
  defaultStatus?: number
  freeFreightPoint?: number
  commentGrowthPoint?: number
  priviledgeFreeFreight?: number
  priviledgeSignIn?: number
  priviledgeComment?: number
  priviledgePromotion?: number
  priviledgeMemberPrice?: number
  priviledgeBirthday?: number
  note?: string
}

export function listMemberLevels(defaultStatus?: number) {
  return unwrap<MemberLevel[]>(request.get('/memberLevel/list', { params: { defaultStatus } }))
}
