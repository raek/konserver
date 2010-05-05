/* The content of this file is in the public domain. */

var users;

function makeOption(label, value) {
  return $('<option value="' + value + '">' + label + '</option>');
}

function onEdit() {
  if (users) {
    $("#edit-panel").show();
  } else {
    $("#loading").show();
    konserver.get(user_coll_uri, function(user_coll) {
      // WIN
      var members = member_coll.members;
      var members_by_user_uri = new Object();
      for (var i = 0; i < members.length; i++) {
        members_by_user_uri[members[i].user] = members[i];
      }
      
      var users = user_coll.users;
      for (i = 0; i < users.length; i++) {
        if (!members_by_user_uri[users[i].uri]) {
          makeOption(users[i].name, users[i].uri).appendTo("#user");
        }
      }
      $("#loading").hide();
      $("#edit-panel").show();
    }, function(xhr) {
      // FAIL
      $("#loading").hide();
      $("#show-panel").show();
      alert("HTTP error: " + xhr.status);
    });
  }
};

