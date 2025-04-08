let api = [];
api.push({
    alias: 'api',
    order: '1',
    desc: 'Web控制器，提供自动化测试系统的API接口  &amp;lt;p&amp;gt;  该控制器提供了测试用例管理、元素库访问和测试用例执行等功能的RESTful接口  &amp;lt;/p&amp;gt;',
    link: 'web控制器，提供自动化测试系统的api接口  &amp;lt;p&amp;gt;  该控制器提供了测试用例管理、元素库访问和测试用例执行等功能的restful接口  &amp;lt;/p&amp;gt;',
    list: []
})
api[0].list.push({
    order: '1',
    methodId: '9c08579f5984651c02503d35c17bd517',
    desc: '获取所有测试用例列表  &lt;p&gt;  该接口会扫描测试用例目录下的所有.txt文件，返回测试用例ID和名称  &lt;/p&gt;',
});
api[0].list.push({
    order: '2',
    methodId: '5dc50880383bddc4c96999fb6052db95',
    desc: '获取指定测试用例的详细内容  &lt;p&gt;  读取指定测试用例文件并解析其中的命令步骤，返回结构化的步骤列表  &lt;/p&gt;',
});
api[0].list.push({
    order: '3',
    methodId: '3d6b64dcea608f4e56d2c8fd091ce2f1',
    desc: '获取UI元素库  &lt;p&gt;  从YAML配置文件中读取元素映射配置，返回所有UI元素的列表  &lt;/p&gt;',
});
api[0].list.push({
    order: '4',
    methodId: '405308ac35a5589abc0bd82637af7b3c',
    desc: '获取所有可用的动作类型列表',
});
api[0].list.push({
    order: '5',
    methodId: '1b7e9b94794b61ee8c6f9846c8a9e9d1',
    desc: '获取按context分组的元素列表',
});
api[0].list.push({
    order: '6',
    methodId: 'ec63315024d4b892aafedadaf5da5c43',
    desc: '保存测试用例  &lt;p&gt;  将前端提交的测试步骤保存为标准格式的测试用例文件  &lt;/p&gt;',
});
api[0].list.push({
    order: '7',
    methodId: 'f8d0ac5a6fa7947ad4d931a13b1cf8b9',
    desc: '执行测试用例  &lt;p&gt;  接收前端提交的测试步骤，将其转换为标准命令格式并执行，返回执行结果日志  &lt;/p&gt;',
});
api[0].list.push({
    order: '8',
    methodId: '4c56b5b7e01af0d94fb17203cf748f30',
    desc: '处理图片上传，识别UI元素并更新元素库  &lt;p&gt;  该接口接收上传的图片，调用图像处理服务识别图片中的UI元素，  更新元素库配置文件，并返回处理结果  &lt;/p&gt;',
});
api[0].list.push({
    order: '9',
    methodId: '1ce1ee74567bce514fbc26d6edc6fba8',
    desc: '获取元素对应的图片路径映射  &lt;p&gt;  返回每个元素ID和其对应的图片URL或文件路径的映射关系  &lt;/p&gt;',
});
api[0].list.push({
    order: '10',
    methodId: 'cd10ed26c1ac359c5c44e60857d32c17',
    desc: '更新元素信息  &lt;p&gt;  接收前端提交的元素更新信息，更新YAML配置文件中的元素定义  &lt;/p&gt;',
});
api[0].list.push({
    order: '11',
    methodId: '6ccdfb0dd5feab555f71b2f2f5867c19',
    desc: '删除元素  &lt;p&gt;  从YAML配置文件中删除指定元素  &lt;/p&gt;',
});
api[0].list.push({
    order: '12',
    methodId: '03f1bfd2487c32e7ebcf00ce0489a94c',
    desc: '获取元素图片  &lt;p&gt;  根据请求参数返回指定元素的图片资源  &lt;/p&gt;',
});
api[0].list.push({
    order: '13',
    methodId: 'abaa2ba49809dc57c48b4a986687a398',
    desc: '更新元素图片  &lt;p&gt;  接收上传的图片文件，更新指定元素的图片路径  &lt;/p&gt;',
});
api[0].list.push({
    order: '14',
    methodId: 'd44dd474722745766153d985786ee68d',
    desc: '创建新元素  &lt;p&gt;  将新元素添加到YAML配置文件中  &lt;/p&gt;',
});
api.push({
    alias: 'BasicController',
    order: '2',
    desc: '',
    link: '',
    list: []
})
api[1].list.push({
    order: '1',
    methodId: 'f969a9a99d8a8116fdde46aed1b0f1ba',
    desc: '',
});
api[1].list.push({
    order: '2',
    methodId: '188e93f1dcde9fb64858e3e4dccb1335',
    desc: '',
});
api[1].list.push({
    order: '3',
    methodId: 'e180d7f396f81de5e8cb2d0c3fd3f346',
    desc: '',
});
api[1].list.push({
    order: '4',
    methodId: 'c92297711cac56cfa4010528070c82fa',
    desc: '',
});
api.push({
    alias: 'PathVariableController',
    order: '3',
    desc: '',
    link: '',
    list: []
})
api[2].list.push({
    order: '1',
    methodId: '4a80fe164205c38fce1fd512dfebd055',
    desc: '',
});
api[2].list.push({
    order: '2',
    methodId: 'de6a4410d3cb8fd5b826a5ab8825388a',
    desc: '',
});
document.onkeydown = keyDownSearch;
function keyDownSearch(e) {
    const theEvent = e;
    const code = theEvent.keyCode || theEvent.which || theEvent.charCode;
    if (code === 13) {
        const search = document.getElementById('search');
        const searchValue = search.value;
        let searchArr = [];
        for (let i = 0; i < api.length; i++) {
            let apiData = api[i];
            const desc = apiData.desc;
            if (desc.toLocaleLowerCase().indexOf(searchValue) > -1) {
                searchArr.push({
                    order: apiData.order,
                    desc: apiData.desc,
                    link: apiData.link,
                    alias: apiData.alias,
                    list: apiData.list
                });
            } else {
                let methodList = apiData.list || [];
                let methodListTemp = [];
                for (let j = 0; j < methodList.length; j++) {
                    const methodData = methodList[j];
                    const methodDesc = methodData.desc;
                    if (methodDesc.toLocaleLowerCase().indexOf(searchValue) > -1) {
                        methodListTemp.push(methodData);
                        break;
                    }
                }
                if (methodListTemp.length > 0) {
                    const data = {
                        order: apiData.order,
                        desc: apiData.desc,
                        alias: apiData.alias,
                        link: apiData.link,
                        list: methodListTemp
                    };
                    searchArr.push(data);
                }
            }
        }
        let html;
        if (searchValue === '') {
            const liClass = "";
            const display = "display: none";
            html = buildAccordion(api,liClass,display);
            document.getElementById('accordion').innerHTML = html;
        } else {
            const liClass = "open";
            const display = "display: block";
            html = buildAccordion(searchArr,liClass,display);
            document.getElementById('accordion').innerHTML = html;
        }
        const Accordion = function (el, multiple) {
            this.el = el || {};
            this.multiple = multiple || false;
            const links = this.el.find('.dd');
            links.on('click', {el: this.el, multiple: this.multiple}, this.dropdown);
        };
        Accordion.prototype.dropdown = function (e) {
            const $el = e.data.el;
            let $this = $(this), $next = $this.next();
            $next.slideToggle();
            $this.parent().toggleClass('open');
            if (!e.data.multiple) {
                $el.find('.submenu').not($next).slideUp("20").parent().removeClass('open');
            }
        };
        new Accordion($('#accordion'), false);
    }
}

function buildAccordion(apiData, liClass, display) {
    let html = "";
    if (apiData.length > 0) {
         for (let j = 0; j < apiData.length; j++) {
            html += '<li class="'+liClass+'">';
            html += '<a class="dd" href="#' + apiData[j].alias + '">' + apiData[j].order + '.&nbsp;' + apiData[j].desc + '</a>';
            html += '<ul class="sectlevel2" style="'+display+'">';
            let doc = apiData[j].list;
            for (let m = 0; m < doc.length; m++) {
                html += '<li><a href="#' + doc[m].methodId + '">' + apiData[j].order + '.' + doc[m].order + '.&nbsp;' + doc[m].desc + '</a> </li>';
            }
            html += '</ul>';
            html += '</li>';
        }
    }
    return html;
}