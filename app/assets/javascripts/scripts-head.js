
function gaSendEvent(element){
    if(window.location.hostname !== 'localhost') {
        ga('send', 'event', element);
    }
};


