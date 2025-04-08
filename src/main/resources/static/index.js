let currentTestCase = null;
let elements = [];
let actions = [];
let groupedElements = {};

// 全局变量
let currentEditingElement = null;
let elementImages = {}; // 存储元素图片路径映射
let lastActiveInputType = null;

// 定义一个全局初始化函数
function initApplication() {
    console.log('初始化应用程序...');
    
    // 加载数据
    loadTestCases();
    loadActions();
    loadGroupedElements();
    loadElementImages();

    console.log('绑定事件处理程序...');
    
    // 绑定添加元素按钮事件
    const addElementButton = document.getElementById('addElementButton');
    if (addElementButton) {
        addElementButton.addEventListener('click', function() {
            console.log('添加元素按钮被点击');
            openAddElementModal();
        });
    } else {
        console.error('未找到添加元素按钮');
    }

    // 绑定添加元素模态窗口中的上传图片功能
    const createElementImageUpload = document.getElementById('createElementImageUpload');
    if (createElementImageUpload) {
        createElementImageUpload.addEventListener('change', function(event) {
            // 确保这里的事件不会传播
            event.stopPropagation();
            previewNewImage(this.files, event);
        });
    }
    
    // 确保模态内容点击不会关闭窗口
    const elementModalContent = document.querySelector('#elementModal .modal-content');
    if (elementModalContent) {
        elementModalContent.addEventListener('click', function(event) {
            event.stopPropagation();
        });
    }
    
    // 绑定添加元素模态窗口中的按钮事件
    const addModalClose = document.querySelector('#addElementModal .close');
    if (addModalClose) {
        addModalClose.addEventListener('click', closeAddElementModal);
    }
    
    const addModalCancel = document.querySelector('#addElementModal .button-secondary');
    if (addModalCancel) {
        // addModalCancel.addEventListener('click', closeAddElementModal);
    }
    
    const addModalSubmit = document.querySelector('#addElementModal .button-primary');
    if (addModalSubmit) {
        addModalSubmit.addEventListener('click', addNewElement);
    }
    
    // 绑定元素模态窗口中的相关按钮事件
    const elementModalClose = document.querySelector('#elementModal .close');
    if (elementModalClose) {
        elementModalClose.addEventListener('click', closeElementModal);
    }



    // 点击模态窗口外部关闭
    window.addEventListener('click', function(event) {
        const elementModal = document.getElementById('elementModal');
        const addElementModal = document.getElementById('addElementModal');
        
        // 获取模态内容元素
        const elementModalContent = document.querySelector('#elementModal .modal-content');
        const addElementModalContent = document.querySelector('#addElementModal .modal-content');
        
        // 如果点击的是模态窗口背景（不是内容区域）才关闭
        if (event.target === addElementModal && !addElementModalContent.contains(event.target)) {
            // 确保不是在处理文件上传
            if (!document.getElementById('createElementImageUpload').files.length) {
                closeAddElementModal();
            }
        }
        
        if (event.target === elementModal && !elementModalContent.contains(event.target)) {
            closeElementModal();
        }
    });

    // 如果没有添加任何步骤（actions已加载完成），就创建一个新的测试用例
    if (document.querySelectorAll('.step-item').length === 0) {
        setTimeout(() => {
            if (actions.length > 0 && document.querySelectorAll('.step-item').length === 0) {
                createNewTestCase();
            }
        }, 500); // 稍微延迟以确保actions已加载
    }

    
    console.log('页面初始化完成，所有事件已绑定');
}


// 页面加载完成后执行
// document.addEventListener('DOMContentLoaded', function() {
//     loadTestCases();
//     loadActions();
//     loadGroupedElements();

//     // 如果没有添加任何步骤（actions已加载完成），就创建一个新的测试用例
//     if (document.querySelectorAll('.step-item').length === 0) {
//         setTimeout(() => {
//             if (actions.length > 0 && document.querySelectorAll('.step-item').length === 0) {
//                 createNewTestCase();
//             }
//         }, 500); // 稍微延迟以确保actions已加载
//     }
// });

// 加载测试用例列表
async function loadTestCases() {
    try {
        const response = await fetch('/api/testcases');
        const testCases = await response.json();
        const testCasesList = document.getElementById('testCasesList');
        testCasesList.innerHTML = '';

        testCases.forEach(testCase => {
            const div = document.createElement('div');
            div.className = 'test-case-item';
            div.textContent = testCase.name;
            div.onclick = () => loadTestCaseContent(testCase.id);
            testCasesList.appendChild(div);
        });
    } catch (error) {
        console.error('加载测试用例列表失败:', error);
    }
}

// 加载测试用例内容
async function loadTestCaseContent(id) {
    try {
        const response = await fetch(`/api/testcases/${id}`);
        const data = await response.json();

        if (data.success) {
            currentTestCase = id;
            document.getElementById('testCaseName').value = id.replace('.txt', '');

            // 更新UI选中状态
            document.querySelectorAll('.test-case-item').forEach(item => {
                item.classList.remove('active');
                if (item.textContent === id.replace('.txt', '')) {
                    item.classList.add('active');
                }
            });

            // 显示测试步骤
            const stepsList = document.getElementById('stepsList');
            stepsList.innerHTML = '';
            data.steps.forEach((step, index) => {
                addStep(step, index + 1);
            });
        } else {
            alert('加载测试用例失败: ' + data.error);
        }
    } catch (error) {
        console.error('加载测试用例内容失败:', error);
        alert('加载测试用例失败');
    }
}

// 加载动作类型列表
async function loadActions() {
    try {
        const response = await fetch('/api/actions');
        actions = await response.json();
        console.log('加载的动作类型:', actions); // 添加日志
        // 确保动作类型列表加载完成后再添加步骤
        if (actions.length > 0) {
            addStep();
        }
    } catch (error) {
        console.error('加载动作类型列表失败:', error);
    }
}

// 加载按context分组的元素列表
async function loadGroupedElements() {
    try {
        const response = await fetch('/api/elements/grouped');
        groupedElements = await response.json();

        // 扁平化元素列表用于搜索
        elements = [];
        Object.entries(groupedElements).forEach(([context, contextElements]) => {
            contextElements.forEach(element => {
                // 确保每个元素都包含context属性
                element.context = context;
                elements.push(element);
            });
        });

        displayGroupedElements(groupedElements);
    } catch (error) {
        console.error('加载分组元素列表失败:', error);
    }
}

// 搜索元素
function searchElements() {
    const searchText = document.getElementById('elementSearch').value.toLowerCase();

    if (!searchText) {
        // 如果搜索框为空，显示所有分组元素
        displayGroupedElements(groupedElements);
        return;
    }

    // 搜索匹配的元素
    const filteredElements = elements.filter(element =>
        element.name.toLowerCase().includes(searchText) ||
        (element.context && element.context.toLowerCase().includes(searchText))
    );

    // 按context重新分组过滤后的元素
    const filteredGrouped = {};
    filteredElements.forEach(element => {
        if (!filteredGrouped[element.context]) {
            filteredGrouped[element.context] = [];
        }
        filteredGrouped[element.context].push(element);
    });

    // 显示过滤结果
    displayGroupedElements(filteredGrouped);
}

// 显示按context分组的元素列表
function displayGroupedElements(groupedElements) {
    const elementsList = document.getElementById('elementsList');
    elementsList.innerHTML = '';

    Object.entries(groupedElements).forEach(([context, elements]) => {
        const contextDiv = document.createElement('div');
        contextDiv.className = 'mb-3';

        const contextHeader = document.createElement('h6');
        contextHeader.textContent = context;
        contextDiv.appendChild(contextHeader);

        elements.forEach(element => {
            const elementDiv = document.createElement('div');
            elementDiv.className = 'element-item';
            elementDiv.textContent = element.name;
            elementDiv.onclick = () => insertElement(element);
            contextDiv.appendChild(elementDiv);
        });

        elementsList.appendChild(contextDiv);
    });
}

// 激活步骤
function activateStep(stepElement) {
    document.querySelectorAll('.step-item').forEach(item => {
        item.classList.remove('active');
    });
    stepElement.classList.add('active');
}

// 插入元素到当前步骤
function insertElement(element) {
    const activeStep = document.querySelector('.step-item.active');
    if (activeStep) {
        const actionSelect = activeStep.querySelector('.step-type');
        const action = actionSelect.value;

        // 组合元素名称
        const elementName = `${element.context}的${element.name}`;

        // 根据不同的动作类型处理
        if (action === '在') {
            // 如果是"在"命令，填充元素输入框
            activeStep.querySelector('.step-input-element').value = elementName;
        } else if (action === '如果') {
            // 检查条件动作是什么
            const conditionAction = activeStep.querySelector('.condition-action').value;
            
            if (conditionAction === '在') {
                // 对于条件中的"在"动作，检查当前活跃的输入框类型
                if (lastActiveInputType === 'condition-element-input') {
                    // 填充元素输入框
                    activeStep.querySelector('.condition-element-input').value = elementName;
                } else if (lastActiveInputType === 'condition-content-input') {
                    // 通常不会直接填充内容输入框，但为了完整性添加此条件
                    activeStep.querySelector('.condition-content-input').value = elementName;
                } else if (lastActiveInputType === 'condition-element') {
                    // 默认填充元素输入框
                    activeStep.querySelector('.condition-element').value = elementName;
                } else {
                    // 默认填充目标输入框
                    activeStep.querySelector('.condition-content').value = elementName;
                    
                    // 设置默认值，下次点击还是填充目标
                    lastActiveInputType = 'condition-content';
                }
            } else {
                // 普通条件动作的处理逻辑
                const conditionElement = activeStep.querySelector('.condition-element');
                const conditionTarget = activeStep.querySelector('.condition-content');
                
                console.log("当前最后活跃的输入框类型：", lastActiveInputType);
                
                if (lastActiveInputType === 'condition-element') {
                    console.log("填充条件元素输入框");
                    conditionElement.value = elementName;
                } else {
                    // 默认填充目标输入框
                    console.log("填充目标输入框");
                    conditionTarget.value = elementName;
                    
                    // 设置默认值，下次点击还是填充目标
                    lastActiveInputType = 'condition-content';
                }
            }
        } else {
            // 其他命令使用标准内容输入框
            const contentInput = activeStep.querySelector('.step-content');
            let content = '';
            switch (action) {
                case '点击':
                case '右键点击':
                case '双击':
                case '鼠标移动到':
                case '出现':
                case '存在':
                    content = elementName;
                    break;
                case '等待':
                case '设置自动等待':
                    content = element.name + '秒';
                    break;
                default:
                    content = element.name;
            }
                    contentInput.value = content;
        }
    } else {
        alert('请先选择一个步骤');
        }
}

// 添加测试步骤
function addStep(stepData = null, stepNumber = null) {
    const template = document.getElementById('stepTemplate');
    const stepElement = template.content.cloneNode(true);
    const stepItem = stepElement.querySelector('.step-item');

    // 创建动作类型下拉框
    const actionSelect = document.createElement('select');
    actionSelect.className = 'step-type';
    actionSelect.onchange = function() {
        const activeStep = document.querySelector('.step-item.active');
        if (activeStep) {
            const contentInput = activeStep.querySelector('.step-content');
            contentInput.value = ''; // 清空内容
        }
    };

    // 添加默认选项
    const defaultOption = document.createElement('option');
    defaultOption.value = '';
    defaultOption.textContent = '请选择动作类型';
    actionSelect.appendChild(defaultOption);

    // 添加动作类型选项
    actions.forEach(action => {
        const option = document.createElement('option');
        option.value = action;
        option.textContent = action === '在' ? '在元素处输入内容' : action;
        actionSelect.appendChild(option);
    });

    // 替换原有的动作类型输入框
    const oldTypeInput = stepItem.querySelector('.step-type');
    oldTypeInput.parentNode.replaceChild(actionSelect, oldTypeInput);

    // 添加动作类型变化事件处理
    actionSelect.addEventListener('change', function() {
        const step = this.closest('.step-item');
        const normalContent = step.querySelector('.step-content');
        const dualInput = step.querySelector('.dual-input');
        const conditionInput = stepItem.querySelector('.condition-input');
        if (conditionInput) {
            // 清空原有内容
            conditionInput.innerHTML = '';

            // 创建新的布局
            const firstRow = document.createElement('div');
            firstRow.style.display = 'flex';
            firstRow.style.alignItems = 'center';
            firstRow.style.marginBottom = '5px';

            // 添加"如果"文本
            const ifLabel = document.createElement('span');
            ifLabel.textContent = '如果';
            ifLabel.style.marginRight = '5px';
            firstRow.appendChild(ifLabel);

            // 添加元素输入框
            const elementInput = document.createElement('input');
            elementInput.type = 'text';
            elementInput.className = 'condition-element';
            elementInput.placeholder = '输入元素';
            elementInput.style.flexGrow = '1';
            elementInput.style.marginRight = '5px';
            // 添加焦点事件
            elementInput.addEventListener('focus', function() {
                console.log("条件元素输入框获得焦点");
                lastActiveInputType = 'condition-element';
            });
            firstRow.appendChild(elementInput);

            // 添加条件类型下拉框
            const conditionType = document.createElement('select');
            conditionType.className = 'condition-type';
            conditionType.style.marginRight = '5px';

            const existOption = document.createElement('option');
            existOption.value = '存在';
            existOption.textContent = '存在';
            conditionType.appendChild(existOption);

            const appearOption = document.createElement('option');
            appearOption.value = '出现';
            appearOption.textContent = '出现';
            conditionType.appendChild(appearOption);

            firstRow.appendChild(conditionType);

            // 添加"则"文本
            const thenLabel = document.createElement('span');
            thenLabel.textContent = '则';
            thenLabel.style.marginRight = '5px';
            firstRow.appendChild(thenLabel);

            conditionInput.appendChild(firstRow);

            // 创建第二行
            const secondRow = document.createElement('div');
            secondRow.style.display = 'flex';
            secondRow.style.alignItems = 'center';

            // 添加动作选择下拉框
            const actionSelect = document.createElement('select');
            actionSelect.className = 'condition-action';
            actionSelect.style.marginRight = '5px';

            // 添加默认选项
            const defaultOption = document.createElement('option');
            defaultOption.value = '';
            defaultOption.textContent = '选择动作';
            actionSelect.appendChild(defaultOption);

            // 添加动作类型选项（除了"如果"以外的所有动作）
            actions.forEach(action => {
                if (action !== '如果') {
                    const option = document.createElement('option');
                    option.value = action;
                    option.textContent = action === '在' ? '在元素处输入内容' : action;
                    actionSelect.appendChild(option);
                }
            });
            // 添加动作选择变更事件
            actionSelect.addEventListener('change', function() {
                // 获取当前选择的动作
                const selectedAction = this.value;

                // 获取包含此动作选择器的行
                const secondRow = this.closest('div');

                // 清除现有的所有输入框
                const existingInputs = secondRow.querySelectorAll('input, div.condition-dual-input');
                existingInputs.forEach(input => input.remove());

                if (selectedAction === '在') {
                    // 创建双输入框容器
                    const dualInput = document.createElement('div');
                    dualInput.className = 'condition-dual-input';
                    dualInput.style.display = 'flex';
                    dualInput.style.flexGrow = '1';

                    // 创建元素输入框
                    const elementInput = document.createElement('input');
                    elementInput.type = 'text';
                    elementInput.className = 'condition-element-input';
                    elementInput.placeholder = '输入元素';
                    elementInput.style.flexGrow = '1';
                    elementInput.style.marginRight = '5px';
                    // 添加焦点事件
                    elementInput.addEventListener('focus', function() {
                        console.log("条件中的元素输入框获得焦点");
                        lastActiveInputType = 'condition-element-input';
                    });
                    dualInput.appendChild(elementInput);

                    // 创建内容输入框
                    const contentInput = document.createElement('input');
                    contentInput.type = 'text';
                    contentInput.className = 'condition-content-input';
                    contentInput.placeholder = '输入内容';
                    contentInput.style.flexGrow = '1';
                    // 添加焦点事件
                    contentInput.addEventListener('focus', function() {
                        console.log("条件中的内容输入框获得焦点");
                        lastActiveInputType = 'condition-content-input';
                    });
                    dualInput.appendChild(contentInput);

                    // 添加到第二行
                    secondRow.appendChild(dualInput);
                } else {
                    // 其他动作类型，使用单一输入框
                    const targetInput = document.createElement('input');
                    targetInput.type = 'text';
                    targetInput.className = 'condition-content';
                    targetInput.placeholder = '输入目标元素';
                    targetInput.style.flexGrow = '1';
                    // 添加焦点事件
                    targetInput.addEventListener('focus', function() {
                        console.log("目标输入框获得焦点");
                        lastActiveInputType = 'condition-content';
                    });
                    secondRow.appendChild(targetInput);
                }
            });
            
            
            console.log("动作类型选项：", actions);

            secondRow.appendChild(actionSelect);

            // 添加目标元素输入框
            const targetInput = document.createElement('input');
            targetInput.type = 'text';
            targetInput.className = 'condition-content';
            targetInput.placeholder = '输入目标元素';
            targetInput.style.flexGrow = '1';
            // 添加焦点事件
            targetInput.addEventListener('focus', function() {
                console.log("目标输入框获得焦点");
                lastActiveInputType = 'condition-content';
            });
            secondRow.appendChild(targetInput);

               

            conditionInput.appendChild(secondRow);
        }



        // 隐藏所有输入框
        normalContent.style.display = 'none';
        dualInput.style.display = 'none';
        conditionInput.style.display = 'none';

        // 根据动作类型显示不同的输入框
        if (this.value === '在') {
            normalContent.style.display = 'none';
            dualInput.style.display = 'block';
            step.querySelector('.step-input-element').style.display = 'block';
            step.querySelector('.step-input-content').style.display = 'block';
        } else if (this.value === '如果') {
            conditionInput.style.display = 'block';
        } else {
            normalContent.style.display = 'block';
        }
    });

    console.log("添加步骤：", stepData);
    if (stepData) {
        console.log("加载步骤数据：", stepData);

        actionSelect.value = stepData.type;
        stepItem.querySelector('.step-content').value = stepData.content;

        // 触发change事件，更新UI结构
        const event = new Event('change');
        actionSelect.dispatchEvent(event);
    
        // 如果是"在"命令，解析内容到双输入框
        if (stepData.type === '在' && stepData.content) {
            const match = stepData.content.match(/(.+?)输入"(.+?)"/);
            if (match) {
                stepItem.querySelector('.step-input-element').value = match[1];
                stepItem.querySelector('.step-input-content').value = match[2];
            }
        }// 如果是"如果"命令，解析内容到条件输入框
        else if (stepData.type === '如果' && stepData.content) {
            // 所有可能的动作类型列表（除了"如果"以外）
            const possibleActions = actions.filter(action => action !== '如果').join('|');

            // 使用所有可能的动作类型构建正则表达式
            const actionPattern = new RegExp(`(.+?)(存在|出现)，则(${possibleActions})(.+)`);

            // 匹配格式：如果[元素][条件]，则[动作][目标]
            const match = stepData.content.match(actionPattern);
            if (match) {
                const [_, element, condition, action, target] = match;
                console.log("匹配到的内容：", match);
            
                // 设置条件输入值
                setTimeout(() => {
                    // 获取生成的DOM结构并填充数据
                    const conditionElement = stepItem.querySelector('.condition-element');
                    const conditionType = stepItem.querySelector('.condition-type');
                    const conditionAction = stepItem.querySelector('.condition-action');
                    
                    if (conditionElement) conditionElement.value = element;
                    if (conditionType) conditionType.value = condition;
                    
                    if (conditionAction) {
                        conditionAction.value = action;

                        // 触发条件动作的change事件，更新相应的输入框结构
                        const actionEvent = new Event('change');
                        conditionAction.dispatchEvent(actionEvent);

                        // 再次延时来确保条件动作相关的DOM结构已生成
                        setTimeout(() => {
                            // 检查动作是否为"在"，处理特殊格式
                            if (action === '在' && target.includes('输入"')) {
                                const targetMatch = target.match(/(.+?)输入"(.+?)"/);
                                if (targetMatch) {
                                    const elementInput = stepItem.querySelector('.condition-element-input');
                                    const contentInput = stepItem.querySelector('.condition-content-input');

                                    if (elementInput) elementInput.value = targetMatch[1];
                                    if (contentInput) contentInput.value = targetMatch[2];
                                }
                            } else {
                                // 普通动作，直接设置目标
                                const conditionContent = stepItem.querySelector('.condition-content');
                                if (conditionContent) conditionContent.value = target;
                            }
                        }, 50); // 短延时确保DOM更新完成
                    }
                }, 50); // 短延时确保DOM更新完成
            }
        }
    }

    // 事件触发一下显示/隐藏相应的输入框
    // if (stepData) {
    //     const event = new Event('change');
    //     actionSelect.dispatchEvent(event);
    // }

    if (stepNumber) {
        stepItem.querySelector('.step-number').textContent = `步骤 ${stepNumber}`;
    } else {
        const steps = document.querySelectorAll('.step-item');
        stepItem.querySelector('.step-number').textContent = `步骤 ${steps.length + 1}`;
    }

    document.getElementById('stepsList').appendChild(stepElement);

    // 激活新添加的步骤
    setTimeout(() => {
        const newStep = document.querySelectorAll('.step-item')[document.querySelectorAll('.step-item').length - 1];
        activateStep(newStep);
    }, 0);
}

// 删除测试步骤
function removeStep(button, event) {
    event.stopPropagation(); // 阻止事件冒泡
    button.closest('.step-item').remove();
    updateStepNumbers();
}

// 更新步骤编号
function updateStepNumbers() {
    document.querySelectorAll('.step-item').forEach((item, index) => {
        item.querySelector('.step-number').textContent = `步骤 ${index + 1}`;
    });
}

// 创建新测试用例
function createNewTestCase() {
    currentTestCase = null;
    document.getElementById('testCaseName').value = '';
    document.getElementById('stepsList').innerHTML = '';
    document.querySelectorAll('.test-case-item').forEach(item => {
        item.classList.remove('active');
    });

    // 添加一个新的空步骤
    addStep();
}

// 保存测试用例
async function saveTestCase() {
    const name = document.getElementById('testCaseName').value;
    if (!name) {
        alert('请输入测试用例名称');
        return;
    }

    const steps = [];
    document.querySelectorAll('.step-item').forEach(item => {
        const type = item.querySelector('.step-type').value;
        let content = '';

        // 根据不同的动作类型处理内容
        if (type === '在') {
            const elementValue = item.querySelector('.step-input-element').value;
            const contentValue = item.querySelector('.step-input-content').value;
            content = `${elementValue}输入"${contentValue}"`;
        } else if (type === '如果') {
            const conditionElement = item.querySelector('.condition-element').value;
            const conditionType = item.querySelector('.condition-type').value;
            const conditionAction = item.querySelector('.condition-action').value;
            let actionContent = '';
            if (conditionAction === '在') {
                // 处理"在"动作的特殊格式
                const elementValue = item.querySelector('.condition-element-input').value;
                const contentValue = item.querySelector('.condition-content-input').value;
                actionContent = `${elementValue}输入"${contentValue}"`;
            } else {
                // 普通动作使用条件内容
                actionContent = item.querySelector('.condition-content').value;
            }
            
            content = `${conditionElement}${conditionType}，则${conditionAction}${actionContent}`;
        } else if (type === '全屏') {
            content = '全屏';
        } else {
            content = item.querySelector('.step-content').value;

            // 为等待和设置自动等待命令补充"秒"
            if ((type === '等待' || type === '设置自动等待') && !content.endsWith('秒')) {
                content += '秒';
            }
        }

        steps.push({
            type: type,
            content: content
        });
    });

    try {
        await fetch('/api/testcases', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                name: name,
                steps: steps
            })
        });

        alert('保存成功');
        loadTestCases();
    } catch (error) {
        console.error('保存测试用例失败:', error);
        alert('保存失败');
    }
}

// 运行测试用例
async function runTestCase() {
    console.log('运行测试用例...');
    const steps = [];
    document.querySelectorAll('.step-item').forEach(item => {
        const type = item.querySelector('.step-type').value;
        let content = '';

        // 根据不同的动作类型处理内容
        if (type === '在') {
            const elementValue = item.querySelector('.step-input-element').value;
            const contentValue = item.querySelector('.step-input-content').value;
            content = `${elementValue}输入"${contentValue}"`;
        } else if (type === '如果') {
            const conditionElement = item.querySelector('.condition-element').value;
            const conditionType = item.querySelector('.condition-type').value;
            const conditionAction = item.querySelector('.condition-action').value;
            let actionContent = '';
            if (conditionAction === '在') {
                // 处理"在"动作的特殊格式
                const elementValue = item.querySelector('.condition-element-input').value;
                const contentValue = item.querySelector('.condition-content-input').value;
                actionContent = `${elementValue}输入"${contentValue}"`;
            } else {
                // 普通动作使用条件内容
                actionContent = item.querySelector('.condition-content').value;
            }
            
            content = `${conditionElement}${conditionType}，则${conditionAction}${actionContent}`;

        } else {
            content = item.querySelector('.step-content').value;

            // 为等待和设置自动等待命令补充"秒"
            if ((type === '等待' || type === '设置自动等待') && !content.endsWith('秒')) {
                content += '秒';
            } else if (type === '输出') {
                // 在内容两边加引号
                content = `"${content}"`;
                console.log("输出内容：", content);

            }
        }

        steps.push({
            type: type,
            content: content
        });
    });

    const resultsDiv = document.getElementById('runResults');
    resultsDiv.innerHTML = '<div class="log-item">开始执行测试用例...</div>';

    try {
        const response = await fetch('/api/run', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                steps: steps
            })
        });

        const result = await response.json();

        // 清空之前的结果
        resultsDiv.innerHTML = '<div class="log-item">测试执行完成</div>';

        // 根据执行结果添加状态标识
        if (result.success === false) {
            resultsDiv.innerHTML += `<div class="log-item error">执行失败 - 错误类型: ${result.errorType || '未知错误'}</div>`;
        } else if (result.warning) {
            resultsDiv.innerHTML += '<div class="log-item warning">执行完成，但有警告</div>';
        } else {
            resultsDiv.innerHTML += '<div class="log-item success">执行成功</div>';
        }

        // 显示所有日志
        if (result.logs && result.logs.length > 0) {
            resultsDiv.innerHTML += '<div class="log-title">详细日志:</div>';

            result.logs.forEach(log => {
                const div = document.createElement('div');
                div.className = 'log-item';

                // 根据日志内容设置样式
                if (log.includes('错误:') || log.includes('执行错误') || log.includes('Error') || log.includes('Exception')) {
                    div.classList.add('error');
                } else if (log.includes('警告:') || log.includes('Warning')) {
                    div.classList.add('warning');
                } else if (log.includes('详细错误信息:') || log.includes('堆栈')) {
                    div.classList.add('stack-trace');
                } else {
                    div.classList.add('info');
                }

                div.textContent = log;
                resultsDiv.appendChild(div);
            });
        }

        // 滚动到底部
        resultsDiv.scrollTop = resultsDiv.scrollHeight;
    } catch (error) {
        console.error('运行测试用例失败:', error);
        resultsDiv.innerHTML = '<div class="log-item error">请求异常: 无法连接到服务器</div>';

        const errorDiv = document.createElement('div');
        errorDiv.className = 'log-item stack-trace';
        errorDiv.textContent = error.message || '未知错误';
        resultsDiv.appendChild(errorDiv);

        resultsDiv.scrollTop = resultsDiv.scrollHeight;
    }
}

// 上传图片并更新元素库
async function uploadImage(files) {
    if (!files || !files.length) return;

    const file = files[0];
    const formData = new FormData();
    formData.append('image', file);

    try {
        // 显示上传中状态
        const resultsDiv = document.getElementById('runResults');
        resultsDiv.innerHTML = '<div class="log-item">正在上传并处理图片...</div>';

        const response = await fetch('/api/upload/image', {
            method: 'POST',
            body: formData
        });

        const result = await response.json();

        if (result.success) {
            // 显示成功消息
            resultsDiv.innerHTML += '<div class="log-item success">图片处理成功! 元素库已更新</div>';

            // 刷新元素库
            await loadGroupedElements();

            // 显示识别到的元素详情
            if (result.elementsFound && result.elementsFound.length > 0) {
                resultsDiv.innerHTML += `<div class="log-item success">共识别到 ${result.elementsFound.length} 个元素</div>`;
                result.elementsFound.forEach(element => {
                    resultsDiv.innerHTML += `<div class="log-item success">- ${element.context}的${element.name}</div>`;
                });
            }
        } else {
            // 显示错误消息
            resultsDiv.innerHTML += `<div class="log-item error">图片处理失败: ${result.error || '未知错误'}</div>`;
        }
    } catch (error) {
        console.error('上传图片失败:', error);
        document.getElementById('runResults').innerHTML += `<div class="log-item error">上传失败: ${error.message}</div>`;
    }

    // 清空文件输入框，允许再次选择相同文件
    document.getElementById('imageUpload').value = '';
}

// 切换元素库选项卡
function switchTab(button, tabId) {
    // 设置激活按钮
    document.querySelectorAll('.tab-button').forEach(btn => {
        btn.classList.remove('active');
    });
    button.classList.add('active');

    // 显示对应内容
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });
    document.getElementById(tabId).classList.add('active');

    // 如果切换到图片视图，确保加载图片
    if (tabId === 'grid-view') {
        loadElementImages();
    }
}

// 加载元素关联的图片
async function loadElementImages() {
    try {
        const response = await fetch('/api/elements/images');
        elementImages = await response.json();

        console.log("结果：")
        console.log(elementImages)

        displayElementsGrid();
    } catch (error) {
        console.error('加载元素图片失败:', error);
    }
}

// 显示元素网格视图
function displayElementsGrid() {
    const elementsGrid = document.getElementById('elementsGrid');
    elementsGrid.innerHTML = '';

    // 调试输出
    console.log("元素图片映射数据：");
    console.log(elementImages);

    Object.entries(groupedElements).forEach(([context, contextElements]) => {
        contextElements.forEach(element => {
            const card = document.createElement('div');
            card.className = 'element-card';
            card.onclick = () => openElementModal(element);

            // 图片容器
            const imgContainer = document.createElement('div');
            imgContainer.className = 'element-image';

            // 检查是否有关联图片
            const elementKey = `${element.context}/${element.name}`;
            if (elementImages[`${element.context}/${element.name}`]) {
                const img = document.createElement('img');
                // 使用API端点获取图片
                img.src = `/api/element/image?path=${encodeURIComponent(elementImages[elementKey])}`;
                img.alt = element.name;
                img.className = 'element-image';
                img.onerror = function() {
                    // 图片加载失败时显示错误提示
                    console.error(`无法加载图片: ${elementImages[elementKey]}`);
                    this.parentNode.innerHTML = '<div style="height:100%;display:flex;align-items:center;justify-content:center;color:#aaa;"><i class="fas fa-exclamation-triangle"></i></div>';
                };
                imgContainer.appendChild(img);
            } else {
                // 无图片占位符
                imgContainer.innerHTML = '<div style="height:100%;display:flex;align-items:center;justify-content:center;color:#aaa;"><i class="fas fa-image"></i></div>';
            }

            const info = document.createElement('div');
            info.className = 'element-info';

            const name = document.createElement('div');
            name.className = 'element-name';
            name.textContent = element.name;

            const contextText = document.createElement('div');
            contextText.className = 'element-context';
            contextText.textContent = element.context;

            info.appendChild(name);
            info.appendChild(contextText);

            card.appendChild(imgContainer);
            card.appendChild(info);

            elementsGrid.appendChild(card);
        });
    });
}

// 更新元素列表显示，添加编辑按钮
function displayGroupedElements(groupedElements) {
    const elementsList = document.getElementById('elementsList');
    elementsList.innerHTML = '';

    Object.entries(groupedElements).forEach(([context, elements]) => {
        const contextDiv = document.createElement('div');
        contextDiv.className = 'mb-3';

        const contextHeader = document.createElement('h6');
        contextHeader.textContent = context;
        contextDiv.appendChild(contextHeader);

        elements.forEach(element => {
            const elementDiv = document.createElement('div');
            elementDiv.className = 'element-list-item';

            const elementInfo = document.createElement('div');
            elementInfo.className = 'element-list-item-info';
            elementInfo.textContent = element.name;
            elementInfo.onclick = () => insertElement(element);

            const actions = document.createElement('div');
            actions.className = 'element-actions';

            const editButton = document.createElement('button');
            editButton.className = 'button button-secondary btn-sm';
            editButton.innerHTML = '<i class="fas fa-pencil-alt"></i>';
            editButton.title = '编辑元素';
            editButton.onclick = (e) => {
                e.stopPropagation();
                openElementModal(element);
            };

            actions.appendChild(editButton);
            elementDiv.appendChild(elementInfo);
            elementDiv.appendChild(actions);

            contextDiv.appendChild(elementDiv);
        });

        elementsList.appendChild(contextDiv);
    });

    // 如果当前在图片视图，也更新图片视图
    if (document.getElementById('grid-view').classList.contains('active')) {
        displayElementsGrid();
    }
}

// 打开元素详情模态窗口
function openElementModal(element) {
    currentEditingElement = element;

    document.getElementById('elementModalTitle').textContent = '元素详情: ' + element.name;
    document.getElementById('editElementContext').value = element.context;

    // 拆分元素名称
    // const elementTypes = ['按钮', '复选框', '下拉框', '图标', '输入框', '标签', '单选框', '开关'];
    const elementTypes = ['button', 'checkbox', 'dropdown', 'icon', 'input', 'label', 'radio', 'switch']
        let baseName = element.name;
    let elementType = '';

    // 检查元素名称是否以已知类型结尾
    for (const type of elementTypes) {
        if (element.name.endsWith(type)) {
            baseName = element.name.substring(0, element.name.length - type.length);
            elementType = type;
            break;
        }
    }

    document.getElementById('editElementBaseName').value = baseName;
    document.getElementById('editElementType').value = elementType;
    document.getElementById('editElementSelector').value = element.cssSelector || '';
    document.getElementById('editElementXpath').value = element.xpath || '';

    // 先重新加载元素图片映射表，确保使用最新数据
    loadElementImages().then(() => {
        // 加载元素预览图
        const previewImage = document.getElementById('elementPreviewImage');
        const elementKey = `${element.context}/${element.name}`;
        
        console.log("尝试加载图片，元素键值:", elementKey);
        console.log("当前可用图片:", Object.keys(elementImages));

        if (elementImages[elementKey]) {
            // 添加时间戳防止浏览器缓存
            const timestamp = new Date().getTime();
            previewImage.src = `/api/element/image?path=${encodeURIComponent(elementImages[elementKey])}&t=${timestamp}`;
            previewImage.style.display = 'block';

            // 设置元素高亮区域 (如果有坐标信息)
            const highlight = document.getElementById('elementHighlight');
            if (element.boundingBox) {
                const { x, y, width, height } = element.boundingBox;
                highlight.style.left = `${x}px`;
                highlight.style.top = `${y}px`;
                highlight.style.width = `${width}px`;
                highlight.style.height = `${height}px`;
                highlight.style.display = 'block';
            } else {
                highlight.style.display = 'none';
            }
        } else {
            console.log("未找到元素图片:", elementKey);
            previewImage.style.display = 'none';
            document.getElementById('elementHighlight').style.display = 'none';
        }

        document.getElementById('elementModal').style.display = 'block';
    });
}

// 关闭元素详情模态窗口
function closeElementModal() {
    document.getElementById('elementModal').style.display = 'none';
    currentEditingElement = null;
}

// 保存元素编辑
async function saveElement() {
    if (!currentEditingElement) return;

    // 获取基本名称和元素类型，并合并
    const baseName = document.getElementById('editElementBaseName').value;
    const elementType = document.getElementById('editElementType').value;
    const fullName = baseName + "_" + elementType;

    const newContext = document.getElementById('editElementContext').value;
     // 使用合并后的完整名称
    const updatedElement = {
        id: currentEditingElement.id, // 如果有ID
        originalContext: currentEditingElement.context,
        originalName: currentEditingElement.name,
        context: newContext,
        name: fullName,
        cssSelector: document.getElementById('editElementSelector').value,
        xpath: document.getElementById('editElementXpath').value
    };

    const resultsDiv = document.getElementById('runResults');
    resultsDiv.innerHTML += '<div class="log-item">正在保存元素信息...</div>';
    resultsDiv.scrollTop = resultsDiv.scrollHeight;

    try {
        // 先更新元素基本信息
        const response = await fetch('/api/elements/update', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(updatedElement)
        });

        const result = await response.json();

        if (result.success) {
            // 检查是否有图片需要上传
            const imageInput = document.getElementById('elementImageUpload');

            if (imageInput.files && imageInput.files.length > 0) {
                resultsDiv.innerHTML += '<div class="log-item">正在更新元素图片...</div>';
                resultsDiv.scrollTop = resultsDiv.scrollHeight;

                // 创建表单数据进行图片上传
                const formData = new FormData();
                formData.append('image', imageInput.files[0]);
                formData.append('context', newContext); // 使用新的上下文
                formData.append('name', baseName + "_" + elementType); // 使用新的名称

                const imageResponse = await fetch('/api/elements/update-image', {
                    method: 'POST',
                    body: formData
                });

                const imageResult = await imageResponse.json();

                if (imageResult.success) {
                    // 重新加载元素图片
                    await loadElementImages();

                    resultsDiv.innerHTML += '<div class="log-item success">元素信息和图片已成功更新</div>';
                } else {
                    resultsDiv.innerHTML += `<div class="log-item error">元素信息已更新，但图片更新失败: ${imageResult.error || '未知错误'}</div>`;
                }

                // 清空图片输入
                imageInput.value = '';
            } else {
                resultsDiv.innerHTML += '<div class="log-item success">元素信息已成功更新</div>';
            }

            // 关闭模态窗口并重新加载元素列表
            closeElementModal();
            await loadGroupedElements();

            resultsDiv.scrollTop = resultsDiv.scrollHeight;
        } else {
            resultsDiv.innerHTML += `<div class="log-item error">更新元素失败: ${result.error || '未知错误'}</div>`;
            resultsDiv.scrollTop = resultsDiv.scrollHeight;
            alert('更新元素失败: ' + (result.error || '未知错误'));
        }
    } catch (error) {
        console.error('保存元素失败:', error);
        resultsDiv.innerHTML += `<div class="log-item error">保存失败: ${error.message}</div>`;
        resultsDiv.scrollTop = resultsDiv.scrollHeight;
        alert('保存失败: ' + error.message);
    }
}

// 删除元素
async function deleteElement() {
    if (!currentEditingElement) return;

    if (!confirm(`确定要删除元素 "${currentEditingElement.context}的${currentEditingElement.name}" 吗？`)) {
        return;
    }

    try {
        const response = await fetch('/api/elements/delete', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                context: currentEditingElement.context,
                name: currentEditingElement.name
            })
        });

        const result = await response.json();

        if (result.success) {
            closeElementModal();
            await loadGroupedElements(); // 重新加载元素库

            const resultsDiv = document.getElementById('runResults');
            resultsDiv.innerHTML += '<div class="log-item success">元素已成功删除</div>';
            resultsDiv.scrollTop = resultsDiv.scrollHeight;
        } else {
            alert('删除元素失败: ' + (result.error || '未知错误'));
        }
    } catch (error) {
        console.error('删除元素失败:', error);
        alert('删除失败: ' + error.message);
    }
}

// 添加这个新函数用于安全地打开文件选择框
function openFileUpload(event) {
    // 阻止事件冒泡
    event.preventDefault();
    event.stopPropagation();
    
    // 获取文件上传元素
    const fileInput = document.getElementById('newElementImageUpload');
    
    // 重新添加change事件监听器（确保只添加一次）
    fileInput.removeEventListener('change', handleFileChange);
    fileInput.addEventListener('change', handleFileChange);
    
    // 打开文件选择对话框
    fileInput.click();
}

// 文件选择变更处理函数
function handleFileChange(event) {
    // 阻止事件冒泡
    event.stopPropagation();
    
    // 处理文件预览
    // previewNewImage(this.files, event);
}

// 更新元素图片预览
async function updateElementImage(files) {
        if (!files || !files.length) return;
        
        const file = files[0];
        
        // 显示选中图片的预览
        const previewImage = document.getElementById('elementPreviewImage');
        previewImage.src = URL.createObjectURL(file);
        previewImage.style.display = 'block';
        
        // 隐藏高亮区域，因为新上传的图片还没有元素坐标信息
        document.getElementById('elementHighlight').style.display = 'none';
        
        // 返回文件输入元素，以便在saveElement函数中使用
        return document.getElementById('elementImageUpload');
}

// 打开添加元素模态窗口
function openAddElementModal() {
        console.log('打开添加元素模态窗口');
        // 清空表单
        document.getElementById('newElementContext').value = '';
        document.getElementById('newElementBaseName').value = '';
        document.getElementById('newElementType').value = 'button';
        document.getElementById('newElementSelector').value = '';
        document.getElementById('newElementXpath').value = '';
        document.getElementById('newElementPreviewImage').src = '';
        document.getElementById('newElementPreviewImage').style.display = 'none';
        
        // 显示模态窗口
        document.getElementById('addElementModal').style.display = 'block';
}

// 关闭添加元素模态窗口
function closeAddElementModal() {
        document.getElementById('addElementModal').style.display = 'none';
}

// 预览新元素图片
async function previewNewImage(files, event) {
    // 确保事件不会冒泡
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }
    
    if (!files || !files.length) return;
    
    const file = files[0];
    
    // 显示选中图片的预览
    const previewImage = document.getElementById('newElementPreviewImage');
    
    // 创建一个新的FileReader
    const reader = new FileReader();
    reader.onload = function(e) {
        previewImage.src = e.target.result;
        previewImage.style.display = 'block';
        
        // 确保模态窗口仍然显示
        document.getElementById('addElementModal').style.display = 'block';
    };
    
    // 读取文件为DataURL
    reader.readAsDataURL(file);
    
    // 额外确保模态窗口保持打开
    setTimeout(() => {
        document.getElementById('addElementModal').style.display = 'block';
    }, 100);

    return  document.getElementById('createElementImageUpload');
}

// 添加新元素
async function addNewElement() {
        // 获取表单数据
        const context = document.getElementById('newElementContext').value;
        const baseName = document.getElementById('newElementBaseName').value;
        const elementType = document.getElementById('newElementType').value;
        const fullName = baseName + "_" + elementType;
        const selector = document.getElementById('newElementSelector').value;
        const xpath = document.getElementById('newElementXpath').value;
        
        if (!context || !baseName) {
            alert('上下文和名称不能为空');
            return;
        }
        
        const newElement = {
            context: context,
            name: fullName,
            cssSelector: selector,
            xpath: xpath,
            locatorType: "image"
        };
        
        const resultsDiv = document.getElementById('runResults');
        resultsDiv.innerHTML += '<div class="log-item">正在创建新元素...</div>';
        resultsDiv.scrollTop = resultsDiv.scrollHeight;
        
        try {
            // 先创建元素
            const response = await fetch('/api/elements/create', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(newElement)
            });
            
            const result = await response.json();
            
            if (result.success) {
                // 检查是否有图片需要上传
                const imageInput = document.getElementById('createElementImageUpload');
                
                if (imageInput.files && imageInput.files.length > 0) {
                    resultsDiv.innerHTML += '<div class="log-item">正在上传元素图片...</div>';
                    resultsDiv.scrollTop = resultsDiv.scrollHeight;
                    
                    // 创建表单数据进行图片上传
                    const formData = new FormData();
                    formData.append('image', imageInput.files[0]);
                    formData.append('context', context);
                    formData.append('name', fullName);
                    
                    const imageResponse = await fetch('/api/elements/update-image', {
                        method: 'POST',
                        body: formData
                    });
                    
                    const imageResult = await imageResponse.json();
                    
                    if (imageResult.success) {
                        resultsDiv.innerHTML += '<div class="log-item success">新元素和图片已成功创建</div>';
                    } else {
                        resultsDiv.innerHTML += `<div class="log-item error">元素已创建，但图片上传失败: ${imageResult.error || '未知错误'}</div>`;
                    }
                    
                    // 清空图片输入
                    imageInput.value = '';
                } else {
                    resultsDiv.innerHTML += '<div class="log-item success">新元素已成功创建</div>';
                }
                
                // 关闭模态窗口并重新加载元素列表
                closeAddElementModal();
                await loadGroupedElements();
                await loadElementImages();
                
                resultsDiv.scrollTop = resultsDiv.scrollHeight;
            } else {
                resultsDiv.innerHTML += `<div class="log-item error">创建元素失败: ${result.error || '未知错误'}</div>`;
                resultsDiv.scrollTop = resultsDiv.scrollHeight;
                alert('创建元素失败: ' + (result.error || '未知错误'));
            }
        } catch (error) {
            console.error('创建元素失败:', error);
            resultsDiv.innerHTML += `<div class="log-item error">创建失败: ${error.message}</div>`;
            resultsDiv.scrollTop = resultsDiv.scrollHeight;
            alert('创建失败: ' + error.message);
        }
}


    
// 页面初始化
document.addEventListener('DOMContentLoaded', initApplication);