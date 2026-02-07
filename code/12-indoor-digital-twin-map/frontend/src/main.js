import * as THREE from 'three';

// 全局变量
let scene, camera, renderer;
let raycaster, mouse;
// 控制变量
let isDragging = false;
let previousMousePosition = { x: 0, y: 0 };
let cameraRotation = { x: 0, y: 0 };
// 存储后端推送的仓库数据
let warehouseData = {};

// 初始化场景
function initScene() {
    // 创建场景
    scene = new THREE.Scene();
    scene.background = new THREE.Color(0x0d1117);

    // 创建相机
    const mapContainer = document.getElementById('map-container');
    camera = new THREE.PerspectiveCamera(60, mapContainer.clientWidth / mapContainer.clientHeight, 0.1, 1000);
    // 设置鸟瞰俯视视角
    camera.position.set(0, 150, 0);
    camera.lookAt(0, 0, 0);

    // 创建渲染器
    const canvas = document.getElementById('map-canvas');
    renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
    renderer.setSize(mapContainer.clientWidth, mapContainer.clientHeight);
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;

    // 添加光源
    const ambientLight = new THREE.AmbientLight(0x404040, 0.6);
    scene.add(ambientLight);

    const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
    directionalLight.position.set(50, 100, 50);
    directionalLight.castShadow = true;
    directionalLight.shadow.mapSize.width = 2048;
    directionalLight.shadow.mapSize.height = 2048;
    directionalLight.shadow.camera.near = 0.5;
    directionalLight.shadow.camera.far = 200;
    directionalLight.shadow.camera.left = -100;
    directionalLight.shadow.camera.right = 100;
    directionalLight.shadow.camera.top = 100;
    directionalLight.shadow.camera.bottom = -100;
    scene.add(directionalLight);

    // 创建仓库区域
    createWarehouseArea();

    // 初始化raycaster和mouse
    raycaster = new THREE.Raycaster();
    mouse = new THREE.Vector2();

    // 动画循环
    animate();
}

// 创建仓库区域
function createWarehouseArea() {
    // 创建地面（草地）
    const groundGeometry = new THREE.PlaneGeometry(800, 800);
    const groundMaterial = new THREE.MeshLambertMaterial({ 
        color: 0x228B22,
        side: THREE.DoubleSide 
    });
    const ground = new THREE.Mesh(groundGeometry, groundMaterial);
    ground.rotation.x = Math.PI / 2;
    ground.position.y = -0.1;
    ground.receiveShadow = true;
    scene.add(ground);

    // 创建仓库建筑群
    // 第一排仓库
    createWarehouse(-100, 0, -50, '1号仓');
    createWarehouse(-30, 0, -50, '2号仓');
    createWarehouse(40, 0, -50, '3号仓');
    createWarehouse(110, 0, -50, '4号仓');

    // 第二排仓库
    createWarehouse(-100, 0, 30, '5号仓');
    createWarehouse(-30, 0, 30, '6号仓');
    createWarehouse(40, 0, 30, '7号仓');
    createWarehouse(110, 0, 30, '8号仓');

    // 创建办公楼
    createOfficeBuilding(-150, 0, 0, '办公楼');

    // 创建道路
    createRoads();

    // 创建树木
    createTrees();

    // 创建蓝色光环
    createBlueRings();
}

// 创建办公楼
function createOfficeBuilding(x, y, z, name) {
    // 办公楼主体
    const buildingGeometry = new THREE.BoxGeometry(60, 25, 30);
    const buildingMaterial = new THREE.MeshLambertMaterial({ color: 0xdddddd });
    const building = new THREE.Mesh(buildingGeometry, buildingMaterial);
    building.position.set(x, 12.5, z);
    building.castShadow = true;
    building.receiveShadow = true;
    scene.add(building);

    // 办公楼窗户
    for (let floor = 0; floor < 5; floor++) {
        for (let i = -25; i <= 25; i += 10) {
            const windowGeometry = new THREE.BoxGeometry(2, 3, 0.2);
            const windowMaterial = new THREE.MeshLambertMaterial({ color: 0x409eff });
            const window = new THREE.Mesh(windowGeometry, windowMaterial);
            window.position.set(x + i, 3 + floor * 5, z + 15.1);
            scene.add(window);
        }
    }

    // 添加标签
    addLabel(x, 30, z, name);
}

// 创建道路
function createRoads() {
    // 主道路
    const roadGeometry = new THREE.PlaneGeometry(400, 15);
    const roadMaterial = new THREE.MeshLambertMaterial({ 
        color: 0x666666,
        side: THREE.DoubleSide 
    });
    const road = new THREE.Mesh(roadGeometry, roadMaterial);
    road.rotation.x = Math.PI / 2;
    road.position.set(0, 0, 0);
    scene.add(road);

    // 支路
    const sideRoadGeometry = new THREE.PlaneGeometry(15, 200);
    const sideRoad1 = new THREE.Mesh(sideRoadGeometry, roadMaterial);
    sideRoad1.rotation.x = Math.PI / 2;
    sideRoad1.position.set(-130, 0, 0);
    scene.add(sideRoad1);

    const sideRoad2 = new THREE.Mesh(sideRoadGeometry, roadMaterial);
    sideRoad2.rotation.x = Math.PI / 2;
    sideRoad2.position.set(140, 0, 0);
    scene.add(sideRoad2);
}

// 创建树木
function createTrees() {
    const treePositions = [
        // 左侧
        [-200, 0, -150], [-200, 0, -100], [-200, 0, -50], [-200, 0, 0], [-200, 0, 50], [-200, 0, 100], [-200, 0, 150],
        [-180, 0, -130], [-180, 0, -80], [-180, 0, -30], [-180, 0, 20], [-180, 0, 70], [-180, 0, 120],
        // 右侧
        [200, 0, -150], [200, 0, -100], [200, 0, -50], [200, 0, 0], [200, 0, 50], [200, 0, 100], [200, 0, 150],
        [180, 0, -130], [180, 0, -80], [180, 0, -30], [180, 0, 20], [180, 0, 70], [180, 0, 120],
        // 前方
        [-150, 0, -200], [-100, 0, -200], [-50, 0, -200], [0, 0, -200], [50, 0, -200], [100, 0, -200], [150, 0, -200],
        // 后方
        [-150, 0, 200], [-100, 0, 200], [-50, 0, 200], [0, 0, 200], [50, 0, 200], [100, 0, 200], [150, 0, 200]
    ];

    for (const position of treePositions) {
        createTree(position[0], position[1], position[2]);
    }
}

// 创建单棵树
function createTree(x, y, z) {
    // 树干
    const trunkGeometry = new THREE.CylinderGeometry(1, 1, 5, 8);
    const trunkMaterial = new THREE.MeshLambertMaterial({ color: 0x8B4513 });
    const trunk = new THREE.Mesh(trunkGeometry, trunkMaterial);
    trunk.position.set(x, y + 2.5, z);
    trunk.castShadow = true;
    trunk.receiveShadow = true;
    scene.add(trunk);

    // 树冠
    const crownGeometry = new THREE.SphereGeometry(3, 16, 16);
    const crownMaterial = new THREE.MeshLambertMaterial({ color: 0x228B22 });
    const crown = new THREE.Mesh(crownGeometry, crownMaterial);
    crown.position.set(x, y + 8, z);
    crown.castShadow = true;
    crown.receiveShadow = true;
    scene.add(crown);
}

// 创建仓库
function createWarehouse(x, y, z, name) {
    // 仓库主体（长方体）
    const warehouseGeometry = new THREE.BoxGeometry(40, 10, 25);
    const warehouseMaterial = new THREE.MeshLambertMaterial({ color: 0xcccccc });
    const warehouse = new THREE.Mesh(warehouseGeometry, warehouseMaterial);
    warehouse.position.set(x, 5, z);
    warehouse.castShadow = true;
    warehouse.receiveShadow = true;
    warehouse.userData = { name: name, type: 'warehouse' };
    scene.add(warehouse);

    // 仓库屋顶（斜坡顶）
    const roofGeometry = new THREE.BoxGeometry(42, 3, 27);
    const roofMaterial = new THREE.MeshLambertMaterial({ color: 0x444444 });
    const roof = new THREE.Mesh(roofGeometry, roofMaterial);
    roof.position.set(x, 11.5, z);
    roof.rotation.x = Math.PI * 0.05; // 轻微倾斜
    roof.castShadow = true;
    roof.receiveShadow = true;
    scene.add(roof);

    // 仓库门
    const doorGeometry = new THREE.BoxGeometry(3, 6, 0.2);
    const doorMaterial = new THREE.MeshLambertMaterial({ color: 0x888888 });
    const door = new THREE.Mesh(doorGeometry, doorMaterial);
    door.position.set(x, 3, z - 12.5);
    scene.add(door);

    // 仓库窗户
    for (let i = -15; i <= 15; i += 10) {
        const windowGeometry = new THREE.BoxGeometry(2, 3, 0.2);
        const windowMaterial = new THREE.MeshLambertMaterial({ color: 0x409eff });
        const window = new THREE.Mesh(windowGeometry, windowMaterial);
        window.position.set(x + i, 5, z + 12.6);
        scene.add(window);
    }

    // 添加仓库编号标签
    addLabel(x, 15, z, name);
}

// 添加标签
function addLabel(x, y, z, text) {
    const canvas = document.createElement('canvas');
    const context = canvas.getContext('2d');
    canvas.width = 256;
    canvas.height = 64;
    context.font = 'Bold 24px Arial';
    context.fillStyle = 'rgba(230, 237, 243, 0.8)';
    context.textAlign = 'center';
    context.fillText(text, 128, 40);

    const texture = new THREE.CanvasTexture(canvas);
    const labelMaterial = new THREE.SpriteMaterial({ 
        map: texture,
        transparent: true 
    });
    const label = new THREE.Sprite(labelMaterial);
    label.position.set(x, y, z);
    label.scale.set(10, 2.5, 1);
    scene.add(label);
}

// 创建蓝色光环
function createBlueRings() {
    // 第一排仓库光环
    createBlueRing(-100, 0, -50, 40);
    createBlueRing(-30, 0, -50, 40);
    createBlueRing(40, 0, -50, 40);
    createBlueRing(110, 0, -50, 40);

    // 第二排仓库光环
    createBlueRing(-100, 0, 30, 40);
    createBlueRing(-30, 0, 30, 40);
    createBlueRing(40, 0, 30, 40);
    createBlueRing(110, 0, 30, 40);
}

// 创建蓝色光环
function createBlueRing(x, z, y, radius) {
    const curve = new THREE.EllipseCurve(
        0, 0, radius, radius, 0, 2 * Math.PI, false, 0
    );
    const points = curve.getPoints(100);
    const geometry = new THREE.BufferGeometry().setFromPoints(points);
    const material = new THREE.LineBasicMaterial({ 
        color: 0x409eff,
        linewidth: 2
    });
    const ring = new THREE.Line(geometry, material);
    ring.position.set(x, y + 0.1, z);
    ring.rotation.x = Math.PI / 2;
    scene.add(ring);

    // 添加动画效果
    ring.userData = { 
        originalScale: ring.scale.clone(),
        pulseDirection: 1,
        pulseSpeed: 0.003 // 减慢动画速度
    };
}

// 动画循环
function animate() {
    requestAnimationFrame(animate);
    
    // 更新动画效果
    updateAnimations();
    
    // 检测鼠标交互
    detectMouseInteraction();
    
    // 相机旋转
    updateCameraRotation();
    
    renderer.render(scene, camera);
}

// 更新相机旋转
function updateCameraRotation() {
    // 计算新的相机位置
    const radius = 150;
    const theta = cameraRotation.y;
    const phi = Math.PI / 2 - cameraRotation.x;
    
    camera.position.x = radius * Math.sin(phi) * Math.cos(theta);
    camera.position.y = radius * Math.cos(phi);
    camera.position.z = radius * Math.sin(phi) * Math.sin(theta);
    
    camera.lookAt(0, 0, 0);
}

// 更新动画效果
function updateAnimations() {
    // 蓝色光环脉冲动画
    scene.traverse(function(object) {
        if (object.userData && object.userData.originalScale) {
            // 更新光环缩放
            const scale = object.scale;
            const originalScale = object.userData.originalScale;
            const pulseDirection = object.userData.pulseDirection;
            const pulseSpeed = object.userData.pulseSpeed;
            
            if (pulseDirection > 0) {
                scale.x += pulseSpeed;
                scale.z += pulseSpeed;
                if (scale.x >= originalScale.x * 1.1) {
                    object.userData.pulseDirection = -1;
                }
            } else {
                scale.x -= pulseSpeed;
                scale.z -= pulseSpeed;
                if (scale.x <= originalScale.x * 0.9) {
                    object.userData.pulseDirection = 1;
                }
            }
        }
    });
}

// 检测鼠标交互
function detectMouseInteraction() {
    if (raycaster && mouse) {
        // 更新raycaster
        raycaster.setFromCamera(mouse, camera);
        
        // 获取所有可交互的物体
        const objects = [];
        // 添加仓库
        scene.traverse(function(object) {
            if (object.userData && object.userData.type === 'warehouse') {
                objects.push(object);
            }
        });
        
        // 检测碰撞
        const intersects = raycaster.intersectObjects(objects);
        
        // 更新鼠标样式
        const mapContainer = document.getElementById('map-container');
        if (intersects.length > 0) {
            mapContainer.style.cursor = 'pointer';
        } else {
            mapContainer.style.cursor = 'default';
        }
    }
}

// 初始化事件监听
function initEventListeners() {
    // 鼠标按下事件
    document.getElementById('map-container').addEventListener('mousedown', function(event) {
        isDragging = true;
        previousMousePosition = {
            x: event.clientX,
            y: event.clientY
        };
    });

    // 鼠标移动事件
    document.getElementById('map-container').addEventListener('mousemove', function(event) {
        // 计算鼠标在归一化设备坐标中的位置
        const mapContainer = document.getElementById('map-container');
        const rect = mapContainer.getBoundingClientRect();
        mouse.x = ((event.clientX - rect.left) / mapContainer.clientWidth) * 2 - 1;
        mouse.y = -((event.clientY - rect.top) / mapContainer.clientHeight) * 2 + 1;

        // 处理拖拽旋转
        if (isDragging) {
            const deltaMove = {
                x: event.clientX - previousMousePosition.x,
                y: event.clientY - previousMousePosition.y
            };

            // 更新相机旋转
            cameraRotation.y += deltaMove.x * 0.01;
            cameraRotation.x += deltaMove.y * 0.01;

            // 限制垂直旋转角度
            cameraRotation.x = Math.max(-Math.PI / 3, Math.min(Math.PI / 3, cameraRotation.x));

            previousMousePosition = {
                x: event.clientX,
                y: event.clientY
            };
        }
    });

    // 鼠标释放事件
    document.getElementById('map-container').addEventListener('mouseup', function() {
        isDragging = false;
    });

    // 鼠标离开事件
    document.getElementById('map-container').addEventListener('mouseleave', function() {
        isDragging = false;
    });

    // 鼠标点击事件
    document.getElementById('map-container').addEventListener('click', function(event) {
        // 计算鼠标在归一化设备坐标中的位置
        const mapContainer = document.getElementById('map-container');
        const rect = mapContainer.getBoundingClientRect();
        mouse.x = ((event.clientX - rect.left) / mapContainer.clientWidth) * 2 - 1;
        mouse.y = -((event.clientY - rect.top) / mapContainer.clientHeight) * 2 + 1;

        // 更新raycaster
        raycaster.setFromCamera(mouse, camera);
        
        // 获取所有可交互的物体
        const objects = [];
        scene.traverse(function(object) {
            if (object.userData && object.userData.type === 'warehouse') {
                objects.push(object);
            }
        });
        
        // 检测碰撞
        const intersects = raycaster.intersectObjects(objects);
        
        if (intersects.length > 0) {
            const clickedObject = intersects[0].object;
            updateInfoPanel(clickedObject);
        }
    });

    // 窗口大小调整
    window.addEventListener('resize', function() {
        const mapContainer = document.getElementById('map-container');
        camera.aspect = mapContainer.clientWidth / mapContainer.clientHeight;
        camera.updateProjectionMatrix();
        renderer.setSize(mapContainer.clientWidth, mapContainer.clientHeight);
    });
}

// 更新信息面板
function updateInfoPanel(object) {
    if (object.userData && object.userData.type === 'warehouse') {
        const name = object.userData.name;
        const data = warehouseData[name];

        if (data) {
            // 更新右侧信息面板
            document.getElementById('warehouse-name').textContent = name;
            document.getElementById('warehouse-temperature').textContent = data.temperature ? (data.temperature + '°C') : '--°C';
            document.getElementById('warehouse-humidity').textContent = data.humidity ? (data.humidity + '%') : '--%';
            document.getElementById('warehouse-status').textContent = data.status || '--';
            document.getElementById('warehouse-grain-type').textContent = data.grainType || '--';
        } else {
            // 如果没有后端数据，使用默认值
            document.getElementById('warehouse-name').textContent = name;
            document.getElementById('warehouse-temperature').textContent = '--°C';
            document.getElementById('warehouse-humidity').textContent = '--%';
            document.getElementById('warehouse-status').textContent = '正常';
            document.getElementById('warehouse-grain-type').textContent = '--';
        }
    }
}

// 初始化WebSocket连接
function initWebSocket() {
    try {
        const ws = new WebSocket('ws://localhost/ws/sensor');

        ws.onopen = function() {
            console.log('WebSocket连接已打开');
        };

        ws.onmessage = function(event) {
            const data = JSON.parse(event.data);
            console.log('收到WebSocket数据:', data);
            
            // 更新仓库数据
            if (data.warehouseData) {
                warehouseData = data.warehouseData;
            }
        };

        ws.onclose = function() {
            console.log('WebSocket连接已关闭');
        };

        ws.onerror = function(error) {
            console.error('WebSocket错误:', error);
        };
    } catch (error) {
        console.error('WebSocket初始化失败:', error);
    }
}

// 初始化应用
function init() {
    initScene();
    initEventListeners();
    initWebSocket();
}

// 启动应用
init();
