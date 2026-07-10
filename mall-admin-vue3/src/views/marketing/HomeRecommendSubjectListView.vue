<template>
  <HomeRecommendPanel
    title="专题推荐"
    description="首页专题推荐管理"
    keyword-placeholder="专题名称"
    name-field="subjectName"
    name-label="专题"
    keyword-key="subjectName"
    :fetcher="(p) => listHomeRecommendSubjects(p as never)"
    :toggle-status="updateHomeRecommendSubjectStatus"
  />
</template>

<script setup lang="ts">
import HomeRecommendPanel from '@/components/business/HomeRecommendPanel.vue'
import { listHomeRecommendSubjects, updateHomeRecommendSubjectStatus } from '@/api/marketing'
</script>
