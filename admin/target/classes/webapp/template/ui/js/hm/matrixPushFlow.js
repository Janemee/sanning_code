var matrixPushFlowJs = function () {
    var handleValidation = function () {
        $('#form').validate({
            errorElement: 'span', //default input error message container
            errorClass: 'help-block', // default input error message class
            focusInvalid: false, // do not focus the last invalid input
            ignore: "",
            rules: {
                "taskContent": {
                    // required: true
                },
                "liveInType": {
                    required: true
                },
                "comment": {
                    required: true
                },
                "taskExpectRunning": {
                    required: true,
                    number: true
                },
                "commentInterval": {
                    required: true,
                    number: true
                },
                "commentTemplateId": {
                    required: true
                },
                "liveInContent": {
                    required: true
                },
                "city": {
                    required: true
                }
            },
            invalidHandler: function (event, validator) { //显示在表单提交错误警报
            },
            highlight: function (element) { // 标出错误的输入
                $(element).closest('.form-group').addClass('has-error'); //设置错误类对照组
            },
            unhighlight: function (element) { // 恢复所做的改变的标出
                $(element).closest('.form-group').removeClass('has-error'); //设置错误类对照组
            },
            success: function (label) {
                label.closest('.form-group').removeClass('has-error'); //类对照组设置成功
                label.remove();
            },
            submitHandler: function (form) {
                var index = parent.layer.getFrameIndex(window.name),
                    successMsg = "操作成功!",
                    failedMsg = "操作失败!",
                    title = "系统提示";
                //loading层提示
                var loading = layer.load(2, {shade: [0.3, '#fff']});
                $.ajax({
                    type: "POST",
                    async: false,
                    data: $(form).serialize(),
                    url: form.action,
                    success: function (data) {
                        console.log(data.code)
                        if (data.code === 200) {
                            var ccc = layer.confirm('任务添加成功', {
                                btn: ['继续添加','关闭']
                            }, function(){
                                parent.notifications('info', "请重新选择设备，避免任务重复", title);
                                layer.close(ccc);
                            }, function(){
                                window.location.reload();
                                layer.close(ccc);
                            });

                        } else {
                            parent.notifications('error', data.msg || failedMsg, title);
                        }
                    },
                    error: function (XMLHttpRequest, textStatus, errorThrown) {
                        parent.notifications('error', '操作失败,请检查网络!', title);
                    },
                    complete: function () {
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