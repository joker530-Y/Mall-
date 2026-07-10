export interface MenuItem {
  id: number
  parentId: number
  title: string
  level: number
  sort: number
  name: string
  icon: string
  hidden: number
  children?: MenuItem[]
}

export interface SidebarItem {
  path: string
  title: string
  icon?: string
  children?: SidebarItem[]
}
