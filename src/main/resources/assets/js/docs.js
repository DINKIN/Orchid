$(function(){
  // build side menu

    console.log("init");
  var html = '';

  $('.bs-docs-section').each(function() {
    var h1 = $(this).find('h1[id]').first(),
        h23 = $(this).find('h2[id], h3[id]');

    if (h1.length) {
      html+= '<li><a href="#' + h1[0].id +'">'+ h1.clone().children().remove().end().text() +'</a>';

      if (h23.length) {
        html+= '<ul class="nav">';
        h23.each(function() {
          html+= '<li><a href="#' + this.id +'">'+ $(this).clone().children().remove().end().text() +'</a></li>';
        });
        html+= '</ul>';
      }

      html+= '</li>';
    }
  });

  if (html == '') {
    $('[role=complementary]').hide();
    $('[role=main]').toggleClass('col-md-9 col-md-12');
  }
  else {
    $('.bs-docs-sidenav').html(html);
  }

  $('[data-bootbox]').on('click', function() {
    var $target = $('#'+$(this).data('bootbox'));
    bootbox.alert({
      title: $target.attr('title'),
      message: $target.html()
    });
  });

  /*!
   * JavaScript for Bootstrap's docs (http://getbootstrap.com)
   * Copyright 2011-2014 Twitter, Inc.
   * Licensed under the Creative Commons Attribution 3.0 Unported License. For
   * details, see http://creativecommons.org/licenses/by/3.0/.
   */
  !function(a){a(function(){if(navigator.userAgent.match(/IEMobile\/10\.0/)){var b=document.createElement("style");b.appendChild(document.createTextNode("@-ms-viewport{width:auto!important}")),document.querySelector("head").appendChild(b)}{var c=a(window),d=a(document.body);a(".navbar").outerHeight(!0)+10}d.scrollspy({target:".bs-docs-sidebar"}),c.on("load",function(){d.scrollspy("refresh")}),a(".bs-docs-container [href=#]").click(function(a){a.preventDefault()}),setTimeout(function(){var b=a(".bs-docs-sidebar");b.affix({offset:{top:function(){var c=b.offset().top,d=parseInt(b.children(0).css("margin-top"),10),e=a(".bs-docs-nav").height();return this.top=c-e-d},bottom:function(){return this.bottom=a(".bs-docs-footer").outerHeight(!0)}}})},100),setTimeout(function(){a(".bs-top").affix()},100)})}(jQuery);
});

function trianglify(color1, color2) {
  var header = $('#content'),
    t = new Trianglify({
      cellsize: 90,
      noiseIntensity: 0,
      x_gradient: [color1, color2]
    }),
    pattern = t.generate(window.screen.width | header.outerWidth(), header.outerHeight()*1.2);

  header.css('background-image', pattern.dataUrl);
}