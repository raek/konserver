/* The content of this file is in the public domain. */

var users;
var users_by_uri;
var members;
var members_by_user_uri;

function link_to_member(uri) {
  var member = members_by_user_uri[uri];
  if (member) {
    return '<a href="' + member.uri + '">' + member.name + '</a>';
  } else {
    var user = users_by_uri[uri];
    return '<i><a href="' + uri + '">' + user.name + '</a></i>';
  }
}

function makeOption(label, value, selected) {
  var selected_attr = '';
  if (selected) {
    selected_attr = ' selected="selected"';
  }
  return $('<option value="' + value + '"' + selected_attr + '>' + label + '</option>');
}

function makeCheckbox(label, id, value, checked) {
  var checked_attr = '';
  if (checked) {
    checked_attr = ' checked="checked"';
  }
  return $('<div><input type="checkbox" id="' + id + '" name="' + id + '" value="' + value + '"' + checked_attr + ' /><label for="' + id + '">' + label + '</label></div>');
}

function fail(xhr) {
  $("#loading").hide();
  $("#show-panel").show();
  alert("HTTP error: " + xhr.status);
}

function onReady() {
  $("#show-panel").hide();
  $("#loading").show();
  konserver.get(user_coll_uri, function(user_coll) {
    users = user_coll.users;
    users_by_uri = new Object();
    for (var i = 0; i < users.length; i++) {
      users_by_uri[users[i].uri] = users[i];
    }
    
    konserver.get(member_coll_uri, function(member_coll) {
      members = member_coll.members;
      members_by_user_uri = new Object();
      for (var i = 0; i < members.length; i++) {
        members_by_user_uri[members[i].user] = members[i];
        makeOption(members[i].name, members[i].user, debt.creditor == members[i].user).appendTo("#creditor");
        makeCheckbox(members[i].name, 'debtor' + i, members[i].user, debt.debtors.indexOf(members[i].user) != -1).appendTo("#debtors");
      }
      
      $("#editor").empty().append(link_to_member(debt.editor));
      $("#show-creditor").empty().append(link_to_member(debt.creditor));
      $("#show-debtors").empty().append(debt.debtors.map(link_to_member).join(", "));
      
      $("#loading").hide();
      $("#show-panel").show();
    }, fail);
  }, fail);
}

