<?php
if ($handle = opendir('./levels/')) {
    while (false !== ($entry = readdir($handle))) {
        if ($entry != "." && $entry != "..") {
            echo "$entry ";
        }
    }
    closedir($handle);
}
?>



<?php
echo 'hello world';
echo $_POST['map'];

$protected = array('1-1','2--6','-4--1','5-14');

if (!in_array($_POST['x'].'-'.$_POST['y'] , $protected) ) {
    $fp = fopen( './levels/'.$_POST['x'].'-'.$_POST['y'].'.php', 'wb' );
    fwrite( $fp, $_POST['map']);
    fclose( $fp );
}

?>






$('#l-save').click ->
      if window.paused is 1
        parameters =
        "map": JSON.stringify(window.map)
        "x" : window.zone[0]
        "y" : window.zone[1]
        $.post(
          './save_level.php'
          parameters
          (data, statusText) ->

          )
        @needs_save = 0
        $('#l-save').fadeTo(100,.3)
