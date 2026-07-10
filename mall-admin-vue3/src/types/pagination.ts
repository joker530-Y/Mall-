export interface Page<T> {
  pageNum: number
  pageSize: number
  total: number
  totalPage?: number
  list: T[]
}
