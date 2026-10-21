<script setup lang="ts">
import AppLayout from '@/layouts/AppLayout.vue'
import Toast from '@/components/ui/Toast.vue'
</script>

<template>
  <AppLayout>
    <RouterView />
  </AppLayout>
  <Toast />
</template>
