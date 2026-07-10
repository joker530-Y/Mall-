import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  addCartItem,
  clearCart,
  deleteCartItems,
  listCartItems,
  updateCartQuantity,
  type CartItem
} from '@/api/modules/cart'
import { useAuthStore } from '@/stores/auth'
import { PortalApiError } from '@/api/http/errors'

export function useCart() {
  const items = ref<CartItem[]>([])
  const loading = ref(false)
  const error = ref('')
  const auth = useAuthStore()

  async function load() {
    loading.value = true
    error.value = ''
    try {
      items.value = await listCartItems()
      await auth.refreshCartCount()
    } catch (err) {
      items.value = []
      error.value = err instanceof PortalApiError ? err.message : '加载购物车失败'
    } finally {
      loading.value = false
    }
  }

  async function add(item: Partial<CartItem>) {
    await addCartItem(item)
    ElMessage.success('已加入购物车')
    await load()
  }

  async function updateQuantity(id: number, quantity: number) {
    const prev = items.value.find((item) => item.id === id)
    if (prev) prev.quantity = quantity
    try {
      await updateCartQuantity(id, quantity)
      await auth.refreshCartCount()
    } catch (err) {
      await load()
      throw err
    }
  }

  async function remove(ids: number[]) {
    await deleteCartItems(ids)
    ElMessage.success('已删除')
    await load()
  }

  async function clear() {
    await clearCart()
    items.value = []
    auth.cartCount = 0
  }

  return { items, loading, error, load, add, updateQuantity, remove, clear }
}
