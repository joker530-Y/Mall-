import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  generateConfirmOrder,
  generateOrder,
  mockPay,
  type ConfirmOrderResult,
  type OrderParam
} from '@/api/modules/checkout'
import { PortalApiError } from '@/api/http/errors'

export function useCheckout() {
  const confirm = ref<ConfirmOrderResult>()
  const loading = ref(false)
  const submitting = ref(false)
  const paying = ref(false)
  const error = ref('')

  async function loadConfirm(
    cartIds: number[],
    options?: { couponId?: number; useIntegration?: number }
  ) {
    loading.value = true
    error.value = ''
    try {
      confirm.value = await generateConfirmOrder({
        cartIds,
        couponId: options?.couponId,
        useIntegration: options?.useIntegration
      })
    } catch (err) {
      confirm.value = undefined
      error.value = err instanceof PortalApiError ? err.message : '加载确认单失败'
    } finally {
      loading.value = false
    }
  }

  async function createOrder(param: OrderParam) {
    if (submitting.value) return undefined
    submitting.value = true
    try {
      const result = await generateOrder(param)
      ElMessage.success('下单成功')
      return result
    } finally {
      submitting.value = false
    }
  }

  async function payMock(orderId: number, payType = 1) {
    if (paying.value) return undefined
    paying.value = true
    try {
      const result = await mockPay(orderId, payType)
      ElMessage.success('模拟支付成功')
      return result
    } finally {
      paying.value = false
    }
  }

  return { confirm, loading, submitting, paying, error, loadConfirm, createOrder, payMock }
}
