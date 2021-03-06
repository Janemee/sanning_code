$(document).ready(function() {
    function e() {
        var e = $("body")[0].style;
        $("#demo_apidemo").colorpicker({
            color: e.backgroundColor
        }).on("changeColor",
        function(o) {
            e.backgroundColor = o.color.toHex()
        }),
        $("#demo_forceformat").colorpicker({
            format: "rgba",
            horizontal: !0
        }),
        $(".demo-auto").colorpicker(),
        $(".disable-button").click(function(e) {
            e.preventDefault(),
            $("#demo_endis").colorpicker("disable")
        }),
        $(".enable-button").click(function(e) {
            e.preventDefault(),
            $("#demo_endis").colorpicker("enable")
        })
    }
    var o = $(".image-crop > img");
    $(o).cropper({
        aspectRatio: 1.618,
        preview: ".img-preview",
        done: function() {}
    });
    var r = $("#inputImage");
    window.FileReader ? r.change(function() {
        var e, i = new FileReader,
        t = this.files;
        t.length && (e = t[0], /^image\/\w+$/.test(e.type) ? (i.readAsDataURL(e), i.onload = function() {
            r.val(""),
            o.cropper("reset", !0).cropper("replace", this.result)
        }) : showMessage("请选择图片文件"))
    }) : r.addClass("hide"),
    $("#download").click(function() {
        window.open(o.cropper("getDataURL"))
    }),
    $("#zoomIn").click(function() {
        o.cropper("zoom", .1)
    }),
    $("#zoomOut").click(function() {
        o.cropper("zoom", -.1)
    }),
    $("#rotateLeft").click(function() {
        o.cropper("rotate", 45)
    }),
    $("#rotateRight").click(function() {
        o.cropper("rotate", -45)
    }),
    $("#setDrag").click(function() {
        o.cropper("setDragMode", "crop")
    }),
    $("#data_1 .input-group.date").datepicker({
        todayBtn: "linked",
        keyboardNavigation: !1,
        forceParse: !1,
        calendarWeeks: !0,
        autoclose: !0
    }),
    $("#data_2 .input-group.date").datepicker({
        startView: 1,
        todayBtn: "linked",
        keyboardNavigation: !1,
        forceParse: !1,
        autoclose: !0,
        format: "yyyy-mm-dd"
    }),
    $("#data_3 .input-group.date").datepicker({
        startView: 2,
        todayBtn: "linked",
        keyboardNavigation: !1,
        forceParse: !1,
        autoclose: !0
    }),
    $("#data_4 .input-group.date").datepicker({
        minViewMode: 1,
        keyboardNavigation: !1,
        forceParse: !1,
        autoclose: !0,
        todayHighlight: !0
    }),
    $("#data_5 .input-daterange").datepicker({
        keyboardNavigation: !1,
        forceParse: !1,
        autoclose: !0
    }); {
        var i = document.querySelector(".js-switch"),
        t = (new Switchery(i, {
            color: "#1AB394"
        }), document.querySelector(".js-switch_2")),
        a = (new Switchery(t, {
            color: "#ED5565"
        }), document.querySelector(".js-switch_3"));
        new Switchery(a, {
            color: "#1AB394"
        })
    }
    $(".i-checks").iCheck({
        checkboxClass: "icheckbox_square-green",
        radioClass: "iradio_square-green"
    }),
    $(".colorpicker-demo1").colorpicker(),
    $(".colorpicker-demo2").colorpicker(),
    $(".colorpicker-demo3").colorpicker(),
    e(),
    $(".demo-destroy").click(function(e) {
        e.preventDefault(),
        $(".demo").colorpicker("destroy"),
        $(".disable-button, .enable-button").off("click")
    }),
    $(".demo-create").click(function(o) {
        o.preventDefault(),
        e()
    });
    var c = $(".back-change")[0].style;
    $("#demo_apidemo").colorpicker({
        color: c.backgroundColor
    }).on("changeColor",
    function(e) {
        c.backgroundColor = e.color.toHex()
    }),
    $(".clockpicker").clockpicker(),
    $('#file-pretty input[type="file"]').prettyFile()
});
var config = {
    ".chosen-select": {},
    ".chosen-select-deselect": {
        allow_single_deselect: !0
    },
    ".chosen-select-no-single": {
        disable_search_threshold: 10
    },
    ".chosen-select-no-results": {
        no_results_text: "Oops, nothing found!"
    },
    ".chosen-select-width": {
        width: "95%"
    }
};
for (var selector in config) $(selector).chosen(config[selector]);
$("#ionrange_1").ionRangeSlider({
    min: 0,
    max: 5e3,
    type: "double",
    prefix: "&yen;",
    maxPostfix: "+",
    prettify: !1,
    hasGrid: !0
}),
$("#ionrange_2").ionRangeSlider({
    min: 0,
    max: 10,
    type: "single",
    step: .1,
    postfix: " 克",
    prettify: !1,
    hasGrid: !0
}),
$("#ionrange_3").ionRangeSlider({
    min: -50,
    max: 50,
    from: 0,
    postfix: "°",
    prettify: !1,
    hasGrid: !0
}),
$("#ionrange_4").ionRangeSlider({
    values: ["一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"],
    type: "single",
    hasGrid: !0
}),
$("#ionrange_5").ionRangeSlider({
    min: 1e4,
    max: 1e5,
    step: 100,
    postfix: " km",
    from: 55e3,
    hideMinMax: !0,
    hideFromTo: !1
}),
$(".dial").knob(),
$("#basic_slider").noUiSlider({
    start: 40,
    behaviour: "tap",
    connect: "upper",
    range: {
        min: 20,
        max: 80
    }
}),
$("#range_slider").noUiSlider({
    start: [40, 60],
    behaviour: "drag",
    connect: !0,
    range: {
        min: 20,
        max: 80
    }
}),
$("#drag-fixed").noUiSlider({
    start: [40, 60],
    behaviour: "drag-fixed",
    connect: !0,
    range: {
        min: 20,
        max: 80
    }
});