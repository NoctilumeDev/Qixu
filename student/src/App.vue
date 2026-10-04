<script setup lang="ts">
  import { onLaunch } from '@dcloudio/uni-app';
  import { initialize } from './runtime';
  onLaunch(() => {
    initialize();
  });
</script>
<style>
  @import './style.css';
</style>
