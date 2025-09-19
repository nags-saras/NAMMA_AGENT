import React, { useState, useEffect } from 'react';
import { Calendar, Clock, Users, AlertTriangle, MessageCircle, Plus, Star, MapPin, Send, Edit, Trash2, UserPlus, Eye, Settings } from 'lucide-react';

// Hugging Face API settings
const HF_API_URL = "https://api-inference.huggingface.co/models/google/flan-t5-base"; 
const HF_API_KEY = "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"; // embedded key


const NaadaAgent = () => {
  const [showSplash, setShowSplash] = useState(true);
  const [events, setEvents] = useState([
    {
      id: 1,
      nameEn: "Yakshagana Night",
      nameKn: "ಯಕ್ಷಗಾನ ರಾತ್ರಿ",
      artist: "Ramesh Bhat",
      coordinator: "Priya Sharma",
      time: "19:00",
      duration: 120,
      status: "confirmed",
      description: "Traditional Yakshagana performance featuring mythological stories with elaborate costumes and makeup.",
      volunteers: [
        { name: "Suresh Kumar", role: "Stage Setup", contact: "9876543210" },
        { name: "Meera Rao", role: "Costume Assistant", contact: "9876543211" },
        { name: "Kiran Patil", role: "Audience Management", contact: "9876543212" }
      ]
    },
    {
      id: 2,
      nameEn: "Classical Dance Performance",
      nameKn: "ಶಾಸ್ತ್ರೀಯ ನೃತ್ಯ",
      artist: "Lakshmi Devi",
      coordinator: "Manjunath Gowda",
      time: "17:30",
      duration: 90,
      status: "confirmed",
      description: "Bharatanatyam performance depicting stories from Indian mythology through graceful movements.",
      volunteers: [
        { name: "Anita Krishnan", role: "Props Management", contact: "9876543213" },
        { name: "Deepak Shetty", role: "Lighting Assistant", contact: "9876543214" }
      ]
    },
    {
      id: 3,
      nameEn: "Folk Music Concert",
      nameKn: "ಜಾನಪದ ಸಂಗೀತ",
      artist: "Karnataka Folk Ensemble",
      coordinator: "Rajesh Nayak",
      time: "20:30",
      duration: 75,
      status: "pending",
      description: "Traditional folk songs from rural Karnataka celebrating harvest festivals and local traditions.",
      volunteers: [
        { name: "Kavitha Murthy", role: "Artist Coordination", contact: "9876543215" },
        { name: "Vinay Kumar", role: "Equipment Setup", contact: "9876543216" }
      ]
    }
  ]);

  const [newEvent, setNewEvent] = useState({
    nameEn: '',
    artist: '',
    coordinator: '',
    time: '',
    duration: 60,
    description: ''
  });

  const [editingVolunteer, setEditingVolunteer] = useState(null);
  const [newVolunteer, setNewVolunteer] = useState({ name: '', role: '', contact: '' });
  const [showEventDetails, setShowEventDetails] = useState(null);

  const techTeam = [
    { name: "Arun Kumar", role: "Sound Engineer", location: "Nokia L5 Cafeteria Tech Room" },
    { name: "Deepa Rao", role: "Audio Specialist", location: "Nokia L5 Cafeteria Tech Room" },
    { name: "Ravi Sharma", role: "Lighting Technician", location: "Nokia L5 Cafeteria Tech Room" },
    { name: "Sneha Patil", role: "Technical Coordinator", location: "Nokia L5 Cafeteria Tech Room" }
  ];

  const [conflicts, setConflicts] = useState([]);
  const [chatMessages, setChatMessages] = useState([
    { type: 'bot', text: 'ನಮಸ್ಕಾರ! I am NAMMA-AGENT. I can help you manage events, volunteers, schedules, and generate content!' },
  ]);
  const [chatInput, setChatInput] = useState('');
  const [generatedContent, setGeneratedContent] = useState('');
  const [activeTab, setActiveTab] = useState('schedule');
  const [language, setLanguage] = useState('en');

  const detectConflicts = () => {
    const newConflicts = [];
    for (let i = 0; i < events.length; i++) {
      for (let j = i + 1; j < events.length; j++) {
        const event1 = events[i];
        const event2 = events[j];
        
        const start1 = parseInt(event1.time.replace(':', ''));
        const end1 = start1 + Math.floor(event1.duration / 60) * 100 + (event1.duration % 60);
        const start2 = parseInt(event2.time.replace(':', ''));
        const end2 = start2 + Math.floor(event2.duration / 60) * 100 + (event2.duration % 60);
        
        if ((start1 <= start2 && end1 > start2) || (start2 <= start1 && end2 > start1)) {
          newConflicts.push({
            event1: event1.nameEn,
            event2: event2.nameEn,
            suggestion: `Move ${event2.nameEn} to ${parseInt(event1.time.split(':')[0]) + 2}:00`
          });
        }
      }
    }
    setConflicts(newConflicts);
  };

  useEffect(() => {
    detectConflicts();
  }, [events]);

  useEffect(() => {
    // Hide splash screen after animation
    const timer = setTimeout(() => {
      setShowSplash(false);
    }, 4000);
    return () => clearTimeout(timer);
  }, []);

  const addEvent = () => {
    if (newEvent.nameEn && newEvent.artist && newEvent.time) {
      const event = {
        id: events.length + 1,
        ...newEvent,
        status: 'pending',
        volunteers: []
      };
      setEvents([...events, event]);
      setNewEvent({
        nameEn: '',
        artist: '',
        coordinator: '',
        time: '',
        duration: 60,
        description: ''
      });
    }
  };

  const updateEvent = (eventId, updatedData) => {
    setEvents(prevEvents => 
      prevEvents.map(e => 
        e.id === eventId ? { ...e, ...updatedData, status: 'updated' } : e
      )
    );
  };

  const addVolunteer = (eventId, volunteer) => {
    setEvents(prevEvents => 
      prevEvents.map(e => 
        e.id === eventId ? { ...e, volunteers: [...e.volunteers, volunteer] } : e
      )
    );
  };

  const removeVolunteer = (eventId, volunteerIndex) => {
    setEvents(prevEvents => 
      prevEvents.map(e => 
        e.id === eventId ? 
          { ...e, volunteers: e.volunteers.filter((_, index) => index !== volunteerIndex) } : e
      )
    );
  };

//  Hugging Face helper
  const callHuggingFace = async (input: string): Promise<string> => {
    try {
      const response = await fetch(HF_API_URL, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${HF_API_KEY}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ inputs: input }),
      });

      const data = await response.json();

      if (Array.isArray(data) && data[0]?.generated_text) {
        return data[0].generated_text;
      } else if (data?.generated_text) {
        return data.generated_text;
      } else {
        return "🤖 Sorry, I couldn’t generate a proper reply.";
      }
    } catch (err) {
      return "⚠️ Error contacting Hugging Face API.";
    }
  };
    

  //  Modified handleChat
  const handleChat = async () => {
    if (!chatInput.trim()) return;

    const userMessage = chatInput;
    setChatMessages(prev => [...prev, { type: 'user', text: userMessage }]);
    setChatInput('');

    let response = '';

  //  Try Hugging Face AI first
    response = await callHuggingFace(userMessage);

  //  If Hugging Face gives nothing useful, fallback to old rule-based logic
    if (!response || response.includes("🤖 Sorry")) {
      response = getRuleBasedResponse(userMessage);
    }

    setChatMessages(prev => [...prev, { type: 'bot', text: response }]);
  };


  const generateContent = async (event, language) => {
    const culturalInfo = {
      'Classical Dance Performance': {
        en: 'Classical Indian dance forms represent centuries of cultural heritage.',
        kn: 'ಶಾಸ್ತ್ರೀಯ ನೃತ್ಯ ಪ್ರಕಾರಗಳು ಶತಮಾನಗಳ ಸಾಂಸ್ಕೃತಿಕ ಪರಂಪರೆಯನ್ನು ಪ್ರತಿನಿಧಿಸುತ್ತವೆ.'
      },
      'Yakshagana Night': {
        en: 'Yakshagana is a traditional theater form from Karnataka.',
        kn: 'ಯಕ್ಷಗಾನವು ಕರ್ನಾಟಕದ ಸಾಂಪ್ರದಾಯಿಕ ರಂಗಭೂಮಿ.'
      },
      'Folk Music Concert': {
        en: 'Karnataka folk music preserves rural community voices.',
        kn: 'ಕರ್ನಾಟಕದ ಜಾನಪದ ಸಂಗೀತವು ಗ್ರಾಮೀಣ ಸಮುದಾಯದ ಧ್ವನಿಯನ್ನು ಸಂರಕ್ಷಿಸುತ್ತದೆ.'
      }
    };

    //  Old logic extracted into helper
  const getRuleBasedResponse = (input: string): string => {
    const lower = input.toLowerCase();

    if (lower.includes("list events")) {
      return `📅 Current Events Schedule:\n${events.map(e => `• ${e.nameEn} - ${e.time}`).join('\n')}`;
    }

    if (lower.includes("conflict")) {
      return "✅ No conflicts detected right now!";
    }

  // ... add your existing keyword checks (reschedule, volunteers, etc.)
    return "🤖 I didn’t understand that. Try: 'list events' or 'generate content'.";
  };


    const info = culturalInfo[event.nameEn] || { en: '', kn: '' };
    let content = '';
    
    if (language === 'kn') {
      content = `🎭 ${event.nameKn || event.nameEn}

✨ ಇಂದು ${event.time} ಗಂಟೆಗೆ ವಿಶೇಷ ಪ್ರದರ್ಶನ!

🎨 ಕಲಾವಿದ: ${event.artist}
👨‍💼 ಸಂಯೋಜಕ: ${event.coordinator}
📍 ಸ್ಥಳ: Nokia L5 Cafeteria - Main Stage

${info.kn}

Nokia L5 ಕೆಫೆಟೇರಿಯಾದಲ್ಲಿ ಎಲ್ಲರೂ ಬನ್ನಿ!

#ನಾದಹಬ್ಬ #ಕನ್ನಡಸಂಸ್ಕೃತಿ #Nokia`;
    } else if (language === 'en') {
      content = `🎭 ${event.nameEn}

✨ Special performance today at ${event.time}!

🎨 Artist: ${event.artist}
👨‍💼 Coordinator: ${event.coordinator}
📍 Venue: Nokia L5 Cafeteria - Main Stage

${info.en}

Join us at Nokia L5 Cafeteria!

#NaadaHabba #KannadaCulture #Nokia`;
    } else {
      content = `🎭 ${event.nameEn} | ${event.nameKn || event.nameEn}

✨ Special performance today at ${event.time}! | ಇಂದು ${event.time} ಗಂಟೆಗೆ ವಿಶೇಷ ಪ್ರದರ್ಶನ!

🎨 Artist | ಕಲಾವಿದ: ${event.artist}
📍 Venue | ಸ್ಥಳ: Nokia L5 Cafeteria

${info.en}
${info.kn}

#NaadaHabba #ನಾದಹಬ್ಬ #Nokia`;
    }
    
    setGeneratedContent(content);
    setActiveTab('content');
    
    setTimeout(() => {
      setChatMessages(prev => [...prev, { 
        type: 'bot', 
        text: `✅ Content generated! Check Content tab.` 
      }]);
    }, 100);
  };

  const generateKannadaContent = (event) => {
    setChatMessages(prev => [...prev, { 
      type: 'user', 
      text: `Generate content for ${event.nameEn}` 
    }, {
      type: 'bot',
      text: `🎭 Which language? Type: "kannada", "english", or "both"`
    }]);
    
    window.selectedEventForContent = event;
    setActiveTab('chat');
  };

  const getStatusColor = (status) => {
    return status === 'confirmed' ? 'text-green-600 bg-green-100' : 
           status === 'updated' ? 'text-blue-600 bg-blue-100' : 'text-yellow-600 bg-yellow-100';
  };

  return (
    <>
      {showSplash && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-gradient-to-br from-red-600 via-yellow-500 to-red-700">
          <div className="text-center">
            {/* 3D Kempegowda Warrior Image */}
            <div className="mb-8 relative">
              <div className="w-40 h-40 mx-auto mb-4 rounded-full bg-gradient-to-br from-yellow-400 to-red-600 p-1 shadow-2xl">
                <div className="w-full h-full rounded-full overflow-hidden bg-gradient-to-br from-yellow-300 to-red-500 shadow-inner relative">
                  {/* Replace this with your Kempegowda image */}
                  <img 
                    src="https://i.pinimg.com/1200x/a4/90/e3/a490e304285e9cea1d1a7416a2ac6843.jpg" 
                    alt="ತಾಯಿ ಭುವನೇಶ್ವರಿಯ ಆಶೀರ್ವಾದದೊಂದಿಗೆ"
                    className="w-full h-full object-cover rounded-full"
                    style={{
                      maskImage: 'radial-gradient(circle, black 60%, transparent 100%)',
                      WebkitMaskImage: 'radial-gradient(circle, black 60%, transparent 100%)'
                    }}
                  />
                  {/* Overlay gradient for blending */}
                  <div className="absolute inset-0 bg-gradient-to-br from-yellow-400/30 via-transparent to-red-600/30 rounded-full mix-blend-overlay"></div>
                </div>
              </div>
              {/* 3D Effect Rings */}
              <div className="absolute inset-0 rounded-full border-4 border-yellow-300 opacity-50 animate-ping"></div>
              <div className="absolute inset-2 rounded-full border-2 border-red-400 opacity-30 animate-pulse"></div>
            </div>
            
            {/* Animated Text */}
            <div className="space-y-4">
              <h1 className="text-6xl font-bold text-yellow-100 animate-bounce" style={{fontFamily: 'Noto Sans Kannada, sans-serif'}}>
                ಕನ್ನಡ ಮೊದಲು
              </h1>
              <h2 className="text-3xl font-semibold text-red-100 animate-pulse">
                KANNADA MODALU
              </h2>
              <div className="text-xl text-yellow-200 animate-fade-in">
                <p>Powered by NAMMA-AGENT</p>
                <p style={{fontFamily: 'Noto Sans Kannada, sans-serif'}}>ನಮ್ಮ ಏಜೆಂಟ್</p>
              </div>
            </div>
            
            {/* Loading Animation */}
            <div className="mt-8 flex justify-center space-x-2">
              <div className="w-3 h-3 bg-yellow-300 rounded-full animate-bounce"></div>
              <div className="w-3 h-3 bg-red-300 rounded-full animate-bounce" style={{animationDelay: '0.1s'}}></div>
              <div className="w-3 h-3 bg-yellow-300 rounded-full animate-bounce" style={{animationDelay: '0.2s'}}></div>
            </div>
          </div>
        </div>
      )}
      
      <div className="max-w-7xl mx-auto p-6 bg-gradient-to-br from-red-50 via-yellow-50 to-red-100 min-h-screen">
        <div className="bg-white rounded-lg shadow-xl">
        <div className="bg-gradient-to-r from-red-600 via-yellow-500 to-red-600 text-white p-6 rounded-t-lg">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold">NAMMA-AGENT</h1>
              <p className="text-yellow-100">AI Assistant for NaadaHabba Festival Management</p>
              <p className="text-sm text-red-200">ನಾದಹಬ್ಬ ಆಯೋಜನೆಗಾಗಿ AI ಸಹಾಯಕ - Nokia L5 Cafeteria</p>
            </div>
            <button
              onClick={() => setLanguage(language === 'en' ? 'kn' : 'en')}
              className="bg-white/20 px-3 py-1 rounded-full text-sm hover:bg-white/30"
            >
              {language === 'en' ? 'ಕನ್ನಡ' : 'English'}
            </button>
          </div>
        </div>

        <div className="border-b">
          <nav className="flex space-x-6 px-6">
            {[
              { id: 'schedule', label: 'Schedule', icon: Calendar, labelKn: 'ಕಾರ್ಯಕ್ರಮ' },
              { id: 'volunteers', label: 'Volunteers', icon: Users, labelKn: 'ಸ್ವಯಂಸೇವಕರು' },
              { id: 'conflicts', label: 'Conflicts', icon: AlertTriangle, labelKn: 'ಸಂಘರ್ಷಗಳು' },
              { id: 'chat', label: 'AI Chat', icon: MessageCircle, labelKn: 'AI ಚಾಟ್' },
              { id: 'content', label: 'Content', icon: Star, labelKn: 'ವಿಷಯ' },
              { id: 'details', label: 'Details', icon: Eye, labelKn: 'ವಿವರಗಳು' },
              { id: 'tech', label: 'Tech Team', icon: Settings, labelKn: 'ತಂತ್ರಜ್ಞ ತಂಡ' }
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center py-4 px-2 border-b-2 font-medium text-sm ${
                  activeTab === tab.id
                    ? 'border-red-500 text-red-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700'
                }`}
              >
                <tab.icon className="w-4 h-4 mr-2" />
                {language === 'en' ? tab.label : tab.labelKn}
              </button>
            ))}
          </nav>
        </div>

        <div className="p-6">
          {activeTab === 'schedule' && (
            <div className="space-y-6">
              <div className="bg-red-50 p-4 rounded-lg">
                <h2 className="text-xl font-semibold mb-4 text-red-800">Add New Event</h2>
                <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
                  <input
                    type="text"
                    placeholder="Event Name"
                    value={newEvent.nameEn}
                    onChange={(e) => setNewEvent({...newEvent, nameEn: e.target.value})}
                    className="border rounded-lg px-3 py-2"
                  />
                  <input
                    type="text"
                    placeholder="Artist Name"
                    value={newEvent.artist}
                    onChange={(e) => setNewEvent({...newEvent, artist: e.target.value})}
                    className="border rounded-lg px-3 py-2"
                  />
                  <input
                    type="text"
                    placeholder="Coordinator"
                    value={newEvent.coordinator}
                    onChange={(e) => setNewEvent({...newEvent, coordinator: e.target.value})}
                    className="border rounded-lg px-3 py-2"
                  />
                  <input
                    type="time"
                    value={newEvent.time}
                    onChange={(e) => setNewEvent({...newEvent, time: e.target.value})}
                    className="border rounded-lg px-3 py-2"
                  />
                  <input
                    type="number"
                    placeholder="Duration (minutes)"
                    value={newEvent.duration}
                    onChange={(e) => setNewEvent({...newEvent, duration: parseInt(e.target.value)})}
                    className="border rounded-lg px-3 py-2"
                  />
                </div>
                <textarea
                  placeholder="Event Description"
                  value={newEvent.description}
                  onChange={(e) => setNewEvent({...newEvent, description: e.target.value})}
                  className="border rounded-lg px-3 py-2 w-full mt-4"
                  rows={2}
                />
                <button
                  onClick={addEvent}
                  className="mt-4 bg-red-600 text-white px-6 py-2 rounded-lg hover:bg-red-700 flex items-center"
                >
                  <Plus className="w-4 h-4 mr-2" />
                  Add Event
                </button>
              </div>

              <div>
                <h2 className="text-xl font-semibold mb-4">Event Schedule - Nokia L5 Cafeteria</h2>
                <div className="space-y-4">
                  {events.map((event) => (
                    <div key={event.id} className="border rounded-lg p-4 bg-white shadow-sm">
                      <div className="flex items-center justify-between">
                        <div className="flex-1">
                          <h3 className="font-semibold text-lg">{event.nameEn}</h3>
                          <p className="text-gray-600">🎨 {event.artist}</p>
                          <p className="text-gray-600">👨‍💼 Coordinator: {event.coordinator}</p>
                        </div>
                        <div className="text-right">
                          <div className="flex items-center text-gray-600 mb-1">
                            <Clock className="w-4 h-4 mr-1" />
                            {event.time} ({event.duration}m)
                          </div>
                          <div className="flex items-center text-gray-600 mb-1">
                            <MapPin className="w-4 h-4 mr-1" />
                            Nokia L5 Cafeteria - Main Stage
                          </div>
                          <span className={`px-2 py-1 rounded-full text-xs ${getStatusColor(event.status)}`}>
                            {event.status}
                          </span>
                        </div>
                      </div>
                      <div className="mt-2 flex items-center justify-between text-sm text-gray-500">
                        <div className="flex items-center">
                          <Users className="w-4 h-4 mr-1" />
                          Volunteers: {event.volunteers.length} assigned
                        </div>
                        <div className="flex gap-2">
                          <button
                            onClick={() => {setShowEventDetails(event); setActiveTab('details')}}
                            className="text-blue-600 hover:text-blue-800 underline"
                          >
                            View Details
                          </button>
                          <button
                            onClick={() => generateKannadaContent(event)}
                            className="text-red-600 hover:text-red-800 underline"
                          >
                            Generate Content
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {activeTab === 'volunteers' && (
            <div className="space-y-6">
              <h2 className="text-xl font-semibold">Volunteer Management</h2>
              
              {events.map((event) => (
                <div key={event.id} className="bg-gray-50 rounded-lg p-4">
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="font-semibold text-lg text-gray-800">
                      {event.nameEn} - Volunteers ({event.volunteers.length})
                    </h3>
                    <button
                      onClick={() => {
                        setNewVolunteer({ name: '', role: '', contact: '' });
                        setEditingVolunteer(`add-${event.id}`);
                      }}
                      className="bg-green-600 text-white px-3 py-1 rounded hover:bg-green-700 flex items-center text-sm"
                    >
                      <UserPlus className="w-4 h-4 mr-1" />
                      Add Volunteer
                    </button>
                  </div>

                  {event.volunteers.length === 0 ? (
                    <p className="text-gray-500 italic">No volunteers assigned yet.</p>
                  ) : (
                    <div className="grid md:grid-cols-2 gap-3">
                      {event.volunteers.map((volunteer, index) => (
                        <div key={index} className="bg-white rounded border p-3">
                          <div className="flex items-center justify-between">
                            <div className="flex-1">
                              <h4 className="font-medium">{volunteer.name}</h4>
                              <p className="text-sm text-gray-600">{volunteer.role}</p>
                              <p className="text-xs text-gray-500">📱 {volunteer.contact}</p>
                            </div>
                            <button
                              onClick={() => removeVolunteer(event.id, index)}
                              className="text-red-600 hover:text-red-800"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}

                  {editingVolunteer === `add-${event.id}` && (
                    <div className="mt-4 bg-blue-50 p-4 rounded border">
                      <h4 className="font-medium mb-3">Add New Volunteer</h4>
                      <div className="grid md:grid-cols-3 gap-3">
                        <input
                          type="text"
                          placeholder="Volunteer Name"
                          value={newVolunteer.name}
                          onChange={(e) => setNewVolunteer({...newVolunteer, name: e.target.value})}
                          className="border rounded px-3 py-2"
                        />
                        <input
                          type="text"
                          placeholder="Role/Responsibility"
                          value={newVolunteer.role}
                          onChange={(e) => setNewVolunteer({...newVolunteer, role: e.target.value})}
                          className="border rounded px-3 py-2"
                        />
                        <input
                          type="tel"
                          placeholder="Contact Number"
                          value={newVolunteer.contact}
                          onChange={(e) => setNewVolunteer({...newVolunteer, contact: e.target.value})}
                          className="border rounded px-3 py-2"
                        />
                      </div>
                      <div className="flex gap-2 mt-3">
                        <button
                          onClick={() => {
                            addVolunteer(event.id, newVolunteer);
                            setEditingVolunteer(null);
                          }}
                          className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700"
                        >
                          Add
                        </button>
                        <button
                          onClick={() => setEditingVolunteer(null)}
                          className="bg-gray-600 text-white px-4 py-2 rounded hover:bg-gray-700"
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}

          {activeTab === 'details' && showEventDetails && (
            <div className="max-w-4xl">
              <div className="bg-gradient-to-r from-red-100 to-yellow-100 rounded-lg p-6 mb-6">
                <h2 className="text-2xl font-bold text-red-800 mb-2">
                  📋 {showEventDetails.nameEn}
                </h2>
                <div className="flex items-center space-x-6 text-red-700">
                  <div className="flex items-center">
                    <Clock className="w-5 h-5 mr-2" />
                    {showEventDetails.time} ({showEventDetails.duration}m)
                  </div>
                  <div className="flex items-center">
                    <MapPin className="w-5 h-5 mr-2" />
                    Nokia L5 Cafeteria - Main Stage
                  </div>
                </div>
              </div>

              <div className="grid md:grid-cols-2 gap-6">
                <div className="bg-white rounded-lg border p-6">
                  <h3 className="text-lg font-semibold mb-4 text-gray-800">🎭 Event Information</h3>
                  <div className="space-y-3">
                    <div>
                      <label className="font-medium text-gray-700">Artist:</label>
                      <p className="text-gray-600">{showEventDetails.artist}</p>
                    </div>
                    <div>
                      <label className="font-medium text-gray-700">Coordinator:</label>
                      <p className="text-gray-600">{showEventDetails.coordinator}</p>
                    </div>
                    <div>
                      <label className="font-medium text-gray-700">Description:</label>
                      <p className="text-gray-600 text-sm">{showEventDetails.description}</p>
                    </div>
                  </div>
                </div>

                <div className="bg-white rounded-lg border p-6">
                  <h3 className="text-lg font-semibold mb-4 text-gray-800">👥 Volunteers ({showEventDetails.volunteers.length})</h3>
                  {showEventDetails.volunteers.length === 0 ? (
                    <p className="text-gray-500 italic">No volunteers assigned yet.</p>
                  ) : (
                    <div className="space-y-3">
                      {showEventDetails.volunteers.map((volunteer, index) => (
                        <div key={index} className="border-l-4 border-green-500 pl-4">
                          <h4 className="font-medium">{volunteer.name}</h4>
                          <p className="text-sm text-green-600">{volunteer.role}</p>
                          <p className="text-xs text-gray-500">📱 {volunteer.contact}</p>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          {activeTab === 'conflicts' && (
            <div>
              <h2 className="text-xl font-semibold mb-4 text-red-600">Scheduling Conflicts</h2>
              {conflicts.length === 0 ? (
                <div className="bg-green-50 border border-green-200 rounded-lg p-6 text-center">
                  <div className="text-green-600 text-lg font-medium">✅ No conflicts detected!</div>
                  <p className="text-green-700 mt-2">All events are properly scheduled</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {conflicts.map((conflict, index) => (
                    <div key={index} className="bg-red-50 border border-red-200 rounded-lg p-4">
                      <div className="flex items-center mb-2">
                        <AlertTriangle className="w-5 h-5 text-red-500 mr-2" />
                        <h3 className="font-semibold text-red-700">Conflict Detected</h3>
                      </div>
                      <p className="text-red-600 mb-2">{conflict.event1} and {conflict.event2} overlap</p>
                      <p className="text-sm text-red-700 bg-red-100 p-2 rounded">
                        <strong>Suggestion:</strong> {conflict.suggestion}
                      </p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === 'chat' && (
            <div>
              <h2 className="text-xl font-semibold mb-4">AI Assistant Chat</h2>
              <div className="border rounded-lg h-96 flex flex-col">
                <div className="flex-1 p-4 overflow-y-auto space-y-3">
                  {chatMessages.map((msg, index) => (
                    <div key={index} className={`flex ${msg.type === 'user' ? 'justify-end' : 'justify-start'}`}>
                      <div className={`max-w-xs lg:max-w-md px-4 py-2 rounded-lg ${
                        msg.type === 'user' ? 'bg-red-500 text-white' : 'bg-gray-100 text-gray-800'
                      }`}>
                        {msg.text}
                      </div>
                    </div>
                  ))}
                </div>
                <div className="border-t p-4 flex">
                  <input
                    type="text"
                    value={chatInput}
                    onChange={(e) => setChatInput(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && handleChat()}
                    placeholder="Ask me about events, volunteers, scheduling..."
                    className="flex-1 border rounded-lg px-3 py-2 mr-2"
                  />
                  <button
                    onClick={handleChat}
                    className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700"
                  >
                    <Send className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'content' && (
            <div>
              <h2 className="text-xl font-semibold mb-4">Generated Content</h2>
              {generatedContent ? (
                <div className="bg-gradient-to-r from-red-50 to-yellow-50 border rounded-lg p-6">
                  <h3 className="font-semibold mb-3 text-red-800">Generated Content:</h3>
                  <div className="bg-white p-4 rounded border whitespace-pre-line" style={{fontFamily: 'Noto Sans Kannada, sans-serif'}}>
                    {generatedContent}
                  </div>
                  <div className="mt-4 flex space-x-2">
                    <button className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700">✓ Approve</button>
                    <button className="bg-gray-600 text-white px-4 py-2 rounded hover:bg-gray-700">✎ Edit</button>
                    <button onClick={() => setGeneratedContent('')} className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700">✗ Discard</button>
                  </div>
                </div>
              ) : (
                <div className="text-center py-12 text-gray-500">
                  <Star className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                  <p>Click "Generate Content" on any event to create social media posts</p>
                </div>
              )}
            </div>
          )}

          {activeTab === 'tech' && (
            <div>
              <h2 className="text-xl font-semibold mb-4">Tech Team - Nokia L5 Cafeteria Tech Room</h2>
              <div className="bg-blue-50 p-4 rounded-lg mb-6">
                <h3 className="font-semibold text-blue-800 mb-2">📍 Control Center</h3>
                <p className="text-blue-700">Nokia L5 Cafeteria Tech Room - Central control for all audio, lighting, and stage equipment</p>
              </div>
              
              <div className="grid md:grid-cols-2 gap-6">
                {techTeam.map((member, index) => (
                  <div key={index} className="bg-white border rounded-lg p-4 shadow-sm">
                    <div className="flex items-center mb-3">
                      <div className="w-10 h-10 bg-blue-500 rounded-full flex items-center justify-center text-white font-bold mr-3">
                        {member.name.charAt(0)}
                      </div>
                      <div>
                        <h3 className="font-semibold">{member.name}</h3>
                        <p className="text-sm text-gray-600">{member.role}</p>
                      </div>
                    </div>
                    <div className="mb-3">
                      <p className="text-sm text-gray-500 mb-1">📍 Location:</p>
                      <p className="text-sm">{member.location}</p>
                    </div>
                    <button className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700 text-sm">
                      📞 Contact {member.name.split(' ')[0]}
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
    </>
  );
};

export default NaadaAgent;