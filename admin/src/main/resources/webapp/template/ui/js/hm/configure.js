var ConfigureJs = function () {
    var handleValidation = function() {
        $('#form').validate({
        	errorElement: 'span', //default input error message container
            errorClass: 'help-block', // default input error message class
            focusInvalid: false, // do not focus the last invalid input
            ignore: "",
            rules : {
				"name": {
					required : true,
					rangelength:[2,50]
				},
				"conf.nid" : {
					required : true,
					minlength : 2
				},
				"conf.value" : {
					required : true,
					minlength : 1
				},
				"conf.type" : {
					required:true
				},
				"conf.remark":{
					minlength : 2
				}
			},
            invalidHandler: function (event, validator) { //显示在表单提交错误警报      
            },
            highlight: function(element){ // 标出错误的输入
                $(element).closest('.form-group').addClass('has-error'); //设置错误类对照组
            },
            unhighlight: function(element){ // 恢复所做的改变的标出
                $(element).closest('.form-group').removeClass('has-error'); //设置错误类对照组
            },
            success: function(label){
                label.closest('.form-group').removeClass('has-error'); //类对照组设置成功
				label.remove();
            },
            submitHandler: function (form) {
            	var index = parent.layer.getFrameIndex(window.name),
            		successMsg = "操作成功!",
                	failedMsg = "操作失败!",
					title = "系统提示";
                //loading层提示
                var loading = layer.load(2, { shade: [0.3,'#fff']});
                $.ajax({
             	   	type: "POST",
             	   	async:false,
             	   	data:$(form).serialize(),
             	   	url: "/s/config/json/saveOrUpdata",
             	   	success: function(data){
             	   		if (data.code === 200){
             	   			// window.parent[winname].$(jqGridId).jqGrid('setGridParam', { url : dataUrl, page : 1 }).trigger("reloadGrid");
                            $(jqGridId).jqGrid('setGridParam', { url : dataUrl, page : 1 }).trigger("reloadGrid");
							parent.notifications('success',successMsg,title);
                        }else{
							parent.notifications('error',data.message,title);
                        }
             	   },
             	   error:function(XMLHttpRequest, textStatus, errorThrown){
					   parent.notifications('error','操作失败,请检查网络!',title);
             	   },
             	   complete:function(){
             		   layer.close(loading);
             		   parent.layer.close(index);
             	   }
             	});
            }
        });
    };
    return {
        init: function () {
        	handleValidation();
        }
    };
}();