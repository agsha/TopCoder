var template;
$(function() {
$.get("/row.template").done(function(data) {
        template = Handlebars.compile(data);
        $('#searchBtn').click(function() {
            searchClicked();
        });
});
});

function searchClicked() {
    //console.log($("#query").val());
    $.post("/problemsList", {query: $("#query").val()}).done(function(data) {
    var obj = JSON.parse(data);
    var tbody = template(obj);
    $("#resultsTable").html(tbody);
    //console.log(template(obj));
    });
}

