<template>
  <div class="price-tag">
    <strong>¥{{ formatMoney(price) }}</strong>
    <span v-if="showOriginal" class="original">¥{{ formatMoney(original) }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatMoney } from '@/utils/format'

const props = defineProps<{ price?: number | string | null; original?: number | string | null }>()
const showOriginal = computed(() => {
  const price = Number(props.price)
  const original = Number(props.original)
  return Number.isFinite(original) && Number.isFinite(price) && original > price
})
</script>

<style scoped>
.price-tag {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.price-tag strong {
  color: var(--color-primary);
  font-size: 18px;
}

.original {
  color: var(--color-muted);
  text-decoration: line-through;
  font-size: 12px;
}
</style>
