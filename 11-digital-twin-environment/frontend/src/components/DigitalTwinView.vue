<template>
  <div id="twin-container">
    <!-- 数据显示面板 -->
    <div class="data-panel">
      <h2>工业数字孪生环境监控</h2>
      <div class="data-item">
        <span class="label">当前温度:</span>
        <span class="value">{{ currentData.temperature.toFixed(1) }}°C</span>
      </div>
      <div class="data-item">
        <span class="label">当前湿度:</span>
        <span class="value">{{ currentData.humidity.toFixed(1) }}%</span>
      </div>
      <div class="data-item" :class="{ 'alert': currentData.alert }">
        <span class="label">告警状态:</span>
        <span class="value">{{ currentData.alert ? '⚠️ 告警' : '✅ 正常' }}</span>
      </div>
    </div>
    
    <!-- 控制按钮 -->
    <div class="control-panel">
      <button @click="resetCamera">重置视角</button>
      <button @click="toggleRotation">
        {{ isRotating ? '停止旋转' : '开始旋转' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import * as THREE from 'three';
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
import { Client } from '@stomp/stompjs';

import { onMounted, onUnmounted, ref, reactive } from 'vue';

// 响应式数据
const currentData = reactive({
  temperature: 25.0,
  humidity: 50.0,
  alert: false
});
const isRotating = ref(false);

// 3D场景变量
let scene, camera, renderer, fog, controls, cube, sensors = [];
let client = null; // WebSocket客户端
let rotationSpeed = 0.01;

onMounted(() => {
  initThree();
  initWebSocket();
});

onUnmounted(() => {
  if (client) {
    client.deactivate();
  }
  if (renderer) {
    renderer.dispose();
  }
  if (controls) {
    controls.dispose();
  }
});

function initThree() {
  // 创建场景
  scene = new THREE.Scene();
  scene.background = new THREE.Color(0x000033); // 深蓝色背景

  // 添加雾效（初始密度低）
  fog = new THREE.Fog(0xffffff, 10, 50);
  fog.density = 0.01;
  scene.fog = fog;

  // 相机
  camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000);
  camera.position.set(20, 10, 20);
  camera.lookAt(0, 0, 0);

  // 渲染器
  renderer = new THREE.WebGLRenderer({ antialias: true });
  renderer.setSize(window.innerWidth, window.innerHeight);
  document.getElementById('twin-container').appendChild(renderer.domElement);

  // 轨道控制器
  controls = new OrbitControls(camera, renderer.domElement);
  controls.enableDamping = true;
  controls.dampingFactor = 0.05;

  // 创建工业环境基础结构
  createIndustrialEnvironment();

  // 灯光
  createLights();

  // 动画循环
  function animate() {
    requestAnimationFrame(animate);
    
    // 更新控制器
    controls.update();
    
    // 自动旋转
    if (isRotating.value) {
      scene.rotation.y += rotationSpeed;
    }
    
    // 传感器动画
    animateSensors();
    
    renderer.render(scene, camera);
  }
  animate();

  // 窗口缩放适配
  window.addEventListener('resize', () => {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
  });
}

function createIndustrialEnvironment() {
  // 创建地板
  const floorGeometry = new THREE.PlaneGeometry(40, 40);
  const floorMaterial = new THREE.MeshStandardMaterial({ 
    color: 0x333333,
    roughness: 0.8
  });
  const floor = new THREE.Mesh(floorGeometry, floorMaterial);
  floor.rotation.x = -Math.PI / 2;
  floor.position.y = -5;
  scene.add(floor);

  // 创建墙壁
  const wallGeometry = new THREE.BoxGeometry(40, 10, 0.1);
  const wallMaterial = new THREE.MeshStandardMaterial({ 
    color: 0x444444,
    roughness: 0.7
  });
  
  // 后墙
  const backWall = new THREE.Mesh(wallGeometry, wallMaterial);
  backWall.position.z = -10;
  backWall.position.y = 0;
  scene.add(backWall);
  
  // 左墙
  const leftWall = new THREE.Mesh(wallGeometry, wallMaterial);
  leftWall.rotation.y = Math.PI / 2;
  leftWall.position.x = -20;
  leftWall.position.y = 0;
  leftWall.position.z = 0;
  scene.add(leftWall);
  
  // 右墙
  const rightWall = new THREE.Mesh(wallGeometry, wallMaterial);
  rightWall.rotation.y = -Math.PI / 2;
  rightWall.position.x = 20;
  rightWall.position.y = 0;
  rightWall.position.z = 0;
  scene.add(rightWall);

  // 创建工业设备
  createIndustrialEquipment();

  // 创建传感器
  createSensors();
}

function createIndustrialEquipment() {
  // 创建主设备
  const equipmentGeometry = new THREE.BoxGeometry(8, 6, 4);
  const equipmentMaterial = new THREE.MeshStandardMaterial({ 
    color: 0x2c3e50,
    roughness: 0.6
  });
  const equipment = new THREE.Mesh(equipmentGeometry, equipmentMaterial);
  equipment.position.set(0, 0, 0);
  scene.add(equipment);
  
  // 设备细节
  const detailGeometry = new THREE.BoxGeometry(2, 4, 2);
  const detailMaterial = new THREE.MeshStandardMaterial({ color: 0x3498db });
  
  const detail1 = new THREE.Mesh(detailGeometry, detailMaterial);
  detail1.position.set(-3, 0, 2.1);
  equipment.add(detail1);
  
  const detail2 = new THREE.Mesh(detailGeometry, detailMaterial);
  detail2.position.set(3, 0, 2.1);
  equipment.add(detail2);
}

function createSensors() {
  // 创建多个传感器
  const sensorPositions = [
    { x: -10, y: 0, z: -5 },
    { x: 10, y: 0, z: -5 },
    { x: 0, y: 3, z: -5 },
    { x: -5, y: 0, z: 5 },
    { x: 5, y: 0, z: 5 }
  ];
  
  sensorPositions.forEach((pos, index) => {
    const sensorGeometry = new THREE.CylinderGeometry(0.5, 0.5, 1, 32);
    const sensorMaterial = new THREE.MeshStandardMaterial({ 
      color: 0x00ff00,
      emissive: 0x004400,
      emissiveIntensity: 0.3
    });
    
    const sensor = new THREE.Mesh(sensorGeometry, sensorMaterial);
    sensor.position.set(pos.x, pos.y, pos.z);
    sensor.rotation.x = Math.PI / 2;
    
    // 添加传感器底座
    const baseGeometry = new THREE.CylinderGeometry(0.8, 1, 0.5, 32);
    const baseMaterial = new THREE.MeshStandardMaterial({ color: 0x666666 });
    const base = new THREE.Mesh(baseGeometry, baseMaterial);
    base.position.set(0, -0.75, 0);
    sensor.add(base);
    
    scene.add(sensor);
    sensors.push({
      mesh: sensor,
      originalColor: 0x00ff00,
      pulseSpeed: 0.05 + index * 0.02
    });
  });
}

function createLights() {
  // 环境光
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.5);
  scene.add(ambientLight);
  
  // 主光源
  const mainLight = new THREE.PointLight(0xffffff, 1);
  mainLight.position.set(10, 15, 10);
  scene.add(mainLight);
  
  // 辅助光源
  const fillLight = new THREE.PointLight(0x4080ff, 0.5);
  fillLight.position.set(-10, 10, -10);
  scene.add(fillLight);
}

function animateSensors() {
  // 传感器呼吸效果
  sensors.forEach((sensor, index) => {
    const time = Date.now() * 0.001 * sensor.pulseSpeed;
    const intensity = 0.3 + Math.sin(time) * 0.2;
    sensor.mesh.material.emissiveIntensity = intensity;
  });
}

function initWebSocket() {
  if (client) return;

  client = new Client({
    brokerURL: 'ws://' + window.location.host + '/ws',
    connectHeaders: {},
    debug: (str) => console.log('STOMP Debug:', str),
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,

    onConnect: (frame) => {
      console.log('✅ WebSocket 连接成功');
      client.subscribe('/topic/environment', (message) => {
        const data = JSON.parse(message.body);
        updateEnvironmentVisual(data);
      });
    },

    onStompError: (frame) => {
      console.error('❌ WebSocket 错误:', frame.headers['message']);
    }
  });

  try {
    client.activate();
  } catch (e) {
    console.error('WebSocket 激活失败:', e);
  }
}

function updateEnvironmentVisual(data) {
  // 更新当前数据
  currentData.temperature = data.temperature;
  currentData.humidity = data.humidity;
  currentData.alert = data.alert;

  // 1. 温度 → 背景色（20℃=绿色，35℃=红色）
  const tempBase = 20;
  const tempRange = 15; // 20～35℃
  let tempRatio = (data.temperature - tempBase) / tempRange;
  tempRatio = Math.max(0, Math.min(1, tempRatio)); // 限制在 [0,1]

  // HSL: 色相 120°(绿) → 0°(红)，饱和度70%，亮度30%～50%
  const hue = 120 * (1 - tempRatio);
  const lightness = 0.3 + tempRatio * 0.2;
  const bgColor = new THREE.Color().setHSL(hue / 360, 0.7, lightness);
  scene.background = bgColor;

  // 2. 湿度 → 雾气密度（0% = 0.01, 100% = 0.1）
  fog.density = 0.01 + (data.humidity / 100) * 0.09;

  // 3. 告警状态 → 页面边框闪烁和传感器颜色变化
  if (data.alert) {
    document.body.style.border = '4px solid #ff0000';
    setTimeout(() => {
      document.body.style.border = '';
    }, 400);
    
    // 传感器告警颜色
    sensors.forEach(sensor => {
      sensor.mesh.material.color.set(0xff0000);
      sensor.mesh.material.emissive.set(0x440000);
    });
  } else {
    // 传感器正常颜色
    sensors.forEach(sensor => {
      sensor.mesh.material.color.set(sensor.originalColor);
      sensor.mesh.material.emissive.set(0x004400);
    });
  }
}

// 控制函数
function resetCamera() {
  camera.position.set(20, 10, 20);
  camera.lookAt(0, 0, 0);
  controls.update();
}

function toggleRotation() {
  isRotating.value = !isRotating.value;
}
</script>

<style scoped>
#twin-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

.data-panel {
  position: absolute;
  top: 20px;
  left: 20px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
  z-index: 100;
}

.data-panel h2 {
  margin-top: 0;
  margin-bottom: 15px;
  font-size: 18px;
  color: #3498db;
}

.data-item {
  margin-bottom: 10px;
  display: flex;
  justify-content: space-between;
}

.data-item .label {
  font-weight: bold;
  color: #ccc;
}

.data-item .value {
  font-size: 16px;
}

.data-item.alert .value {
  color: #ff4444;
  font-weight: bold;
}

.control-panel {
  position: absolute;
  bottom: 20px;
  left: 20px;
  display: flex;
  gap: 10px;
  z-index: 100;
}

.control-panel button {
  background: rgba(0, 0, 0, 0.7);
  color: white;
  border: 1px solid #3498db;
  padding: 10px 15px;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.control-panel button:hover {
  background: rgba(52, 152, 219, 0.8);
  transform: translateY(-2px);
}
</style>